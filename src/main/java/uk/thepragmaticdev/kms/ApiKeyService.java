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
   * TODO.
   * 
   * @param accountService   TODO
   * @param apiKeyRepository TODO
   * @param apiKeyLogService TODO
   * @param passwordEncoder  TODO
   * @param apiKeyLimit      TODO
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

  public List<ApiKey> findAll(String username) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    return apiKeyRepository.findAllByAccountId(authenticatedAccount.getId());
  }

  /**
   * TODO.
   * 
   * @param username TODO
   * @param apiKey   TODO
   * @return
   */
  @Transactional
  public ApiKey create(String username, ApiKey apiKey) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    if (apiKeyRepository.countByAccountId(authenticatedAccount.getId()) < apiKeyLimit) {
      apiKey.setId(null);
      apiKey.setAccount(authenticatedAccount);
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
   * TODO.
   * 
   * @param username TODO
   * @param id       TODO
   * @param apiKey   TODO
   * @return
   */
  @Transactional
  public ApiKey update(String username, long id, ApiKey apiKey) {
    apiKey.setId(id);
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    ApiKey persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(apiKey.getId(), authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    persistedApiKey.setName(apiKey.getName());
    updateScope(persistedApiKey, apiKey.getScope()); // TODO same as updateEnabled
    updateAccessPolicies(persistedApiKey, apiKey.getAccessPolicies()); // TODO same as updateEnabled
    updateEnabled(persistedApiKey, apiKey.getEnabled());
    persistedApiKey.setModifiedDate(OffsetDateTime.now());
    return apiKeyRepository.save(persistedApiKey);
  }

  private void updateScope(ApiKey persistedApiKey, Scope scope) {
    persistedApiKey.getScope().setImage(scope.getImage());
    persistedApiKey.getScope().setGif(scope.getGif());
    persistedApiKey.getScope().setText(scope.getText());
    persistedApiKey.getScope().setVideo(scope.getVideo());
  }

  private void updateAccessPolicies(ApiKey persistedApiKey, Collection<AccessPolicy> accessPolicies) {
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
   * TODO.
   * 
   * @param username TODO
   * @param id       TODO
   */
  @Transactional
  public void delete(String username, long id) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    ApiKey persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    apiKeyRepository.delete(persistedApiKey);
    apiKeyLogService.deleted(persistedApiKey.getId());
  }

  /**
   * TODO.
   * 
   * @param pageable TODO
   * @param username TODO
   * @param id       TODO
   * @return
   */
  public Page<ApiKeyLog> log(Pageable pageable, String username, long id) {
    Account authenticatedAccount = accountService.findAuthenticatedAccount(username);
    ApiKey persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.NOT_FOUND));
    return apiKeyLogService.findAllByApiKeyId(pageable, persistedApiKey.getId());
  }

  /**
   * TODO.
   * 
   * @param response TODO
   * @param username TODO
   * @param id       TODO
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
      // TODO Auto-generated catch block
    } catch (IOException e) {
      // TODO Auto-generated catch block
    }
  }

  /**
   * TODO.
   * 
   * @param username TODO
   * @return
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
