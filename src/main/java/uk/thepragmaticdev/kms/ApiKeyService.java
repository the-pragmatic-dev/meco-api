package uk.thepragmaticdev.kms;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.ApiKeyCode;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.log.key.ApiKeyLog;
import uk.thepragmaticdev.log.key.ApiKeyLogService;

@Service
public class ApiKeyService {

  private AccountService accountService;

  private ApiKeyLogService apiKeyLogService;

  private ApiKeyRepository apiKeyRepository;

  private PasswordEncoder passwordEncoder;

  private final int apiKeyLimit;

  /**
   * Service for creating, updating and deleting accounts. Activity logs related
   * to an authorised key may also be downloaded.
   * 
   * @param accountService   The service for retrieving account information
   * @param apiKeyRepository The data access repository for keys
   * @param apiKeyLogService The service for accessing key logs
   * @param passwordEncoder  The service for encoding passwords
   * @param apiKeyLimit      The maximum number of keys allowed by account
   */
  @Autowired
  public ApiKeyService(//
      AccountService accountService, //
      ApiKeyRepository apiKeyRepository, //
      ApiKeyLogService apiKeyLogService, //
      PasswordEncoder passwordEncoder, //
      @Value("${kms.api-key-limit}") int apiKeyLimit) {
    this.accountService = accountService;
    this.apiKeyRepository = apiKeyRepository;
    this.apiKeyLogService = apiKeyLogService;
    this.passwordEncoder = passwordEncoder;
    this.apiKeyLimit = apiKeyLimit;
  }

  /**
   * Find all keys owned by the authenticaed account.
   * 
   * @param username The authenticated account username
   * @return A list of all keys owned by the account
   */
  public List<ApiKey> findAll(String username) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    return apiKeyRepository.findAllByAccountId(authenticatedAccount.getId());
  }

  /**
   * Create a new key.
   * 
   * @param username The authenticated account username
   * @param apiKey   The new key to be created
   * @return A newly created key
   */
  @Transactional
  public ApiKey create(String username, ApiKey apiKey) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    if (apiKeyRepository.countByAccountId(authenticatedAccount.getId()) < apiKeyLimit) {
      apiKey.setId(null);
      apiKey.setAccount(authenticatedAccount);
      apiKey.setName(apiKey.getName());
      apiKey.setPrefix(generatePrefix());
      apiKey.setKey(apiKey.getPrefix().concat(".").concat(generateApiKey()));
      apiKey.setHash(encodeApiKey(apiKey.getKey()));
      apiKey.setCreatedDate(OffsetDateTime.now());
      apiKey.setEnabled(true);
      ApiKey persistedApiKey = apiKeyRepository.save(apiKey);
      apiKeyLogService.created(persistedApiKey.getId());
      return persistedApiKey;
    }
    throw new ApiException(ApiKeyCode.API_KEY_LIMIT);
  }

  /**
   * Update all mutable fields of a key owned by an authenticated account.
   * 
   * @param username The authenticated account username
   * @param id       The id of the key to be updated
   * @param apiKey   A key with the desired values
   * @return The updated key
   */
  @Transactional
  public ApiKey update(String username, long id, ApiKey apiKey) {
    apiKey.setId(id);
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    ApiKey persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(apiKey.getId(), authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    persistedApiKey.setName(apiKey.getName());
    updateScope(persistedApiKey, apiKey.getScope());
    updateAccessPolicies(persistedApiKey, apiKey.getAccessPolicies()); // TODO same as updateEnabled
    updateEnabled(persistedApiKey, apiKey.getEnabled());
    persistedApiKey.setModifiedDate(OffsetDateTime.now());
    return apiKeyRepository.save(persistedApiKey);
  }

  private void updateScope(ApiKey persistedApiKey, Scope scope) {
    Scope persistedScope = persistedApiKey.getScope();
    if (persistedScope.getImage() != scope.getImage()) { // update image scope
      apiKeyLogService.scope(persistedApiKey.getId(), "image", scope.getImage());
      persistedScope.setImage(scope.getImage());
    }
    if (persistedScope.getGif() != scope.getGif()) { // update gif scope
      apiKeyLogService.scope(persistedApiKey.getId(), "gif", scope.getGif());
      persistedScope.setGif(scope.getGif());
    }
    if (persistedScope.getText() != scope.getText()) { // update text scope
      apiKeyLogService.scope(persistedApiKey.getId(), "text", scope.getText());
      persistedScope.setText(scope.getText());
    }
    if (persistedScope.getVideo() != scope.getVideo()) { // update video scope
      apiKeyLogService.scope(persistedApiKey.getId(), "video", scope.getVideo());
      persistedScope.setVideo(scope.getVideo());
    }
  }

  private void updateAccessPolicies(ApiKey persistedApiKey, Collection<AccessPolicy> accessPolicies) {
    // TODO not happy with this
    persistedApiKey.getAccessPolicies().clear();
    persistedApiKey.getAccessPolicies().addAll(accessPolicies);
  }

  private void updateEnabled(ApiKey persistedApiKey, boolean enabled) {
    if (persistedApiKey.getEnabled() != enabled) {
      apiKeyLogService.enabled(persistedApiKey.getId(), enabled);
      persistedApiKey.setEnabled(enabled);
    }
  }

  /**
   * Delete a key owned by an authenticated account.
   * 
   * @param username The authenticated account username
   * @param id       The id of the key to be deleted
   */
  @Transactional
  public void delete(String username, long id) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    ApiKey persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    apiKeyLogService.delete(persistedApiKey.getId());
    apiKeyRepository.delete(persistedApiKey);
  }

  /**
   * Find the latest logs for the requested key id.
   * 
   * @param pageable The pagination information
   * @param username The authenticated account username
   * @param id       The id of the key requesting logs
   * @return A page of the latest key logs
   */
  public Page<ApiKeyLog> log(Pageable pageable, String username, long id) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    ApiKey persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    return apiKeyLogService.findAllByApiKeyId(pageable, persistedApiKey.getId());
  }

  /**
   * Download all logs for the requested key id as a CSV file.
   * 
   * @param response The servlet response
   * @param username The authenticated account username
   * @param id       The id of the key requesting logs
   */
  public void downloadLog(HttpServletResponse response, String username, long id) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    ApiKey persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    try {
      StatefulBeanToCsv<ApiKeyLog> writer = new StatefulBeanToCsvBuilder<ApiKeyLog>(response.getWriter())
          .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER).withSeparator(CSVWriter.DEFAULT_SEPARATOR)
          .withOrderedResults(true).build();
      writer.write(apiKeyLogService.findAllByApiKeyId(persistedApiKey.getId()));
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
      throw new ApiException(CriticalCode.CSV_WRITING_ERROR);
    } catch (IOException e) {
      throw new ApiException(CriticalCode.PRINT_WRITER_IO_ERROR);
    }
  }

  /**
   * Counts the amount of keys owned by the authenticated account.
   * 
   * @param username The authenticated account username
   * @return A count of keys owned by the authenticated account
   */
  public long count(String username) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    return apiKeyRepository.countByAccountId(authenticatedAccount.getId());
  }

  /**
   * TODO.
   * 
   * @param rawApiKey     TODO
   * @param encodedApiKey TODO
   * @return
   */
  public boolean authenticate(String rawApiKey, String encodedApiKey) {
    String apiKey = rawApiKey.substring(rawApiKey.indexOf(".") + 1);
    return passwordEncoder.matches(apiKey, encodedApiKey);
  }

  private String generatePrefix() {
    return RandomStringUtils.randomAlphanumeric(7);
  }

  private String generateApiKey() {
    UUID uuid = UUID.randomUUID();
    return Base64.getEncoder().withoutPadding().encodeToString(uuid.toString().getBytes());
  }

  private String encodeApiKey(String apiKey) {
    return passwordEncoder.encode(apiKey);
  }
}
