package uk.thepragmaticdev.kms;

import com.opencsv.ICSVWriter;
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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.ApiKeyCode;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.log.key.ApiKeyLog;
import uk.thepragmaticdev.log.key.ApiKeyLogService;
import uk.thepragmaticdev.log.security.SecurityLogService;

@Service
public class ApiKeyService {

  private final AccountService accountService;

  private final ApiKeyRepository apiKeyRepository;

  private final ApiKeyLogService apiKeyLogService;

  private final SecurityLogService securityLogService;

  private final EmailService emailService;

  private final PasswordEncoder passwordEncoder;

  private final int apiKeyLimit;

  /**
   * Service for creating, updating and deleting accounts. Activity logs related
   * to an authorised key may also be downloaded.
   * 
   * @param accountService     The service for retrieving account information
   * @param apiKeyRepository   The data access repository for keys
   * @param apiKeyLogService   The service for accessing key logs
   * @param securityLogService The service for accessing security logs
   * @param emailService       The service for sending emails
   * @param passwordEncoder    The service for encoding passwords
   * @param apiKeyLimit        The maximum number of keys allowed by account
   */
  @Autowired
  public ApiKeyService(//
      AccountService accountService, //
      ApiKeyRepository apiKeyRepository, //
      ApiKeyLogService apiKeyLogService, //
      SecurityLogService securityLogService, //
      EmailService emailService, //
      PasswordEncoder passwordEncoder, //
      @Value("${kms.api-key-limit}") int apiKeyLimit) {
    this.accountService = accountService;
    this.apiKeyRepository = apiKeyRepository;
    this.apiKeyLogService = apiKeyLogService;
    this.securityLogService = securityLogService;
    this.emailService = emailService;
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
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
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
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    if (apiKeyRepository.countByAccountId(authenticatedAccount.getId()) < apiKeyLimit) {
      apiKey.setId(null);
      apiKey.setAccount(authenticatedAccount);
      apiKey.setName(apiKey.getName());
      apiKey.setPrefix(generatePrefix());
      apiKey.setKey(apiKey.getPrefix().concat(".").concat(generateApiKey()));
      apiKey.setHash(encodeApiKey(apiKey.getKey()));
      apiKey.setCreatedDate(OffsetDateTime.now());
      apiKey.setEnabled(true);
      setAccessPolicies(apiKey);
      var persistedApiKey = apiKeyRepository.save(apiKey);
      apiKeyLogService.created(persistedApiKey);
      securityLogService.createKey(authenticatedAccount, persistedApiKey);
      emailService.sendKeyCreated(authenticatedAccount, persistedApiKey);
      return persistedApiKey;
    }
    throw new ApiException(ApiKeyCode.API_KEY_LIMIT);
  }

  private void setAccessPolicies(ApiKey apiKey) {
    if (apiKey.getAccessPolicies() == null) {
      return;
    }
    apiKey.getAccessPolicies().forEach(p -> p.setApiKey(apiKey));
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
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    var persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(apiKey.getId(), authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    persistedApiKey.setName(apiKey.getName());
    updateScope(persistedApiKey, apiKey.getScope());
    updateAccessPolicies(persistedApiKey, apiKey.getAccessPolicies());
    updateEnabled(persistedApiKey, apiKey.getEnabled());
    persistedApiKey.setModifiedDate(OffsetDateTime.now());
    return apiKeyRepository.save(persistedApiKey);
  }

  private void updateScope(ApiKey persistedApiKey, Scope scope) {
    var persistedScope = persistedApiKey.getScope();
    if (persistedScope.getImage() != scope.getImage()) { // update image scope
      apiKeyLogService.scope(persistedApiKey, "image", scope.getImage());
      persistedScope.setImage(scope.getImage());
    }
    if (persistedScope.getGif() != scope.getGif()) { // update gif scope
      apiKeyLogService.scope(persistedApiKey, "gif", scope.getGif());
      persistedScope.setGif(scope.getGif());
    }
    if (persistedScope.getText() != scope.getText()) { // update text scope
      apiKeyLogService.scope(persistedApiKey, "text", scope.getText());
      persistedScope.setText(scope.getText());
    }
    if (persistedScope.getVideo() != scope.getVideo()) { // update video scope
      apiKeyLogService.scope(persistedApiKey, "video", scope.getVideo());
      persistedScope.setVideo(scope.getVideo());
    }
  }

  private void updateAccessPolicies(ApiKey persistedApiKey, Collection<AccessPolicy> newPolicies) {
    var existingPolicies = persistedApiKey.getAccessPolicies();
    // check to see if new and exitsing policies match, if so skip
    if (!CollectionUtils.isEqualCollection(existingPolicies, newPolicies)) {
      // add new polcies or update existing policies
      for (var newPolicy : newPolicies) {
        var existingPolicy = existingPolicies.stream().filter(p -> p.getRange().equals(newPolicy.getRange()))
            .findFirst();
        if (existingPolicy.isPresent()) {
          existingPolicy.get().setName(newPolicy.getName());
          apiKeyLogService.accessPolicy(persistedApiKey, "updated", newPolicy.getRange());
        } else {
          newPolicy.setApiKey(persistedApiKey);
          existingPolicies.add(newPolicy);
          apiKeyLogService.accessPolicy(persistedApiKey, "created", newPolicy.getRange());
        }
      }
      // remove existing policies that don't exist in new policies
      existingPolicies.removeIf(p -> !policyMatches(persistedApiKey, p, newPolicies));
    }
  }

  private boolean policyMatches(ApiKey persistedApiKey, AccessPolicy existingPolicy,
      Collection<AccessPolicy> newPolicies) {
    var exists = newPolicies.stream().anyMatch(p -> p.getRange().equals(existingPolicy.getRange()));
    if (!exists) {
      apiKeyLogService.accessPolicy(persistedApiKey, "deleted", existingPolicy.getRange());
    }
    return exists;
  }

  private void updateEnabled(ApiKey persistedApiKey, boolean enabled) {
    if (persistedApiKey.getEnabled() != enabled) {
      apiKeyLogService.enabled(persistedApiKey, enabled);
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
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    var persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    apiKeyRepository.delete(persistedApiKey);
    securityLogService.deleteKey(authenticatedAccount, persistedApiKey);
    emailService.sendKeyDeleted(authenticatedAccount, persistedApiKey);
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
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    var persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    return apiKeyLogService.findAllByApiKeyId(pageable, persistedApiKey);
  }

  /**
   * Download all logs for the requested key id as a CSV file.
   * 
   * @param response The servlet response
   * @param username The authenticated account username
   * @param id       The id of the key requesting logs
   */
  public void downloadLog(HttpServletResponse response, String username, long id) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    var persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    try {
      var writer = new StatefulBeanToCsvBuilder<ApiKeyLog>(response.getWriter())
          .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER).withSeparator(ICSVWriter.DEFAULT_SEPARATOR)
          .withOrderedResults(true).build();
      writer.write(apiKeyLogService.findAllByApiKeyId(persistedApiKey));
      apiKeyLogService.downloadLogs(persistedApiKey);
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException ex) {
      throw new ApiException(CriticalCode.CSV_WRITING_ERROR);
    } catch (IOException ex) {
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
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
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
    var apiKey = rawApiKey.substring(rawApiKey.indexOf('.') + 1);
    return passwordEncoder.matches(apiKey, encodedApiKey);
  }

  private String generatePrefix() {
    return RandomStringUtils.randomAlphanumeric(7);
  }

  private String generateApiKey() {
    var uuid = UUID.randomUUID();
    return Base64.getEncoder().withoutPadding().encodeToString(uuid.toString().getBytes());
  }

  private String encodeApiKey(String apiKey) {
    return passwordEncoder.encode(apiKey);
  }
}
