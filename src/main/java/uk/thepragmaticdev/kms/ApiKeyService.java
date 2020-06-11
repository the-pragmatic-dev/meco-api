package uk.thepragmaticdev.kms;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
   * Service for creating, updating and deleting api keys. Activity logs related
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
   * Find key owned by the authenticaed account.
   * 
   * @param username The authenticated account username
   * @param id       The id of the key to find
   * @return A key owned by the account
   */
  public ApiKey findById(String username, long id) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    return apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.API_KEY_NOT_FOUND));
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
      apiKey.setAccount(authenticatedAccount);
      apiKey.setName(apiKey.getName());
      apiKey.setPrefix(generatePrefix());
      apiKey.setKey(apiKey.getPrefix().concat(".").concat(generateKey()));
      apiKey.setHash(hash(apiKey.getKey()));
      apiKey.setCreatedDate(OffsetDateTime.now());
      setEnabled(apiKey);
      setScope(apiKey);
      setAccessPolicies(apiKey);
      var persistedApiKey = apiKeyRepository.save(apiKey);
      apiKeyLogService.created(persistedApiKey);
      securityLogService.createKey(authenticatedAccount, persistedApiKey);
      emailService.sendKeyCreated(authenticatedAccount, persistedApiKey);
      return persistedApiKey;
    }
    throw new ApiException(ApiKeyCode.API_KEY_LIMIT);
  }

  private void setEnabled(ApiKey apiKey) {
    if (apiKey.getEnabled() == null) {
      apiKey.setEnabled(false);
    }
  }

  private void setScope(ApiKey apiKey) {
    if (apiKey.getScope() == null) {
      apiKey.setScope(new Scope());
    }
    apiKey.getScope().setApiKey(apiKey);
  }

  private void setAccessPolicies(ApiKey apiKey) {
    if (apiKey.getAccessPolicies() == null) {
      apiKey.setAccessPolicies(Collections.emptyList());
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
        .orElseThrow(() -> new ApiException(ApiKeyCode.API_KEY_NOT_FOUND));
    updateName(persistedApiKey, apiKey.getName());
    updateScope(persistedApiKey, apiKey.getScope());
    updateAccessPolicies(persistedApiKey, apiKey.getAccessPolicies());
    updateEnabled(persistedApiKey, apiKey.getEnabled());
    persistedApiKey.setModifiedDate(OffsetDateTime.now());
    return apiKeyRepository.save(persistedApiKey);
  }

  private void updateName(ApiKey persistedApiKey, String name) {
    if (name == null) {
      return;
    }
    if (!persistedApiKey.getName().equals(name)) {
      apiKeyLogService.name(persistedApiKey);
      persistedApiKey.setName(name);
    }
  }

  private void updateScope(ApiKey persistedApiKey, Scope scope) {
    if (scope == null) {
      return;
    }
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
    if (newPolicies == null) {
      return;
    }
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

  private void updateEnabled(ApiKey persistedApiKey, Boolean enabled) {
    if (enabled == null) {
      return;
    }
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
        .orElseThrow(() -> new ApiException(ApiKeyCode.API_KEY_NOT_FOUND));
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
        .orElseThrow(() -> new ApiException(ApiKeyCode.API_KEY_NOT_FOUND));
    return apiKeyLogService.findAllByApiKeyId(pageable, persistedApiKey);
  }

  /**
   * Download all logs for the requested key id as a CSV file.
   * 
   * @param writer   The csv writer
   * @param username The authenticated account username
   * @param id       The id of the key requesting logs
   */
  public void downloadLog(StatefulBeanToCsv<ApiKeyLog> writer, String username, long id) {
    var authenticatedAccount = accountService.findAuthenticatedAccount(username);
    var persistedApiKey = apiKeyRepository.findOneByIdAndAccountId(id, authenticatedAccount.getId())
        .orElseThrow(() -> new ApiException(ApiKeyCode.API_KEY_NOT_FOUND));
    try {
      writer.write(apiKeyLogService.findAllByApiKeyId(persistedApiKey));
      apiKeyLogService.downloadLogs(persistedApiKey);
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException ex) {
      throw new ApiException(CriticalCode.CSV_WRITING_ERROR);
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
   * Verify the encoded hashed key obtained from storage matches the submitted raw
   * key after it too is encoded. The stored key itself is never decoded.
   * 
   * @param rawKey    The raw, unencoded key from request
   * @param hashedKey The encoded key from storage
   * @return True if the keys match, false if they do not
   */
  public boolean isAuthentic(String rawKey, String hashedKey) {
    return passwordEncoder.matches(rawKey, hashedKey);
  }

  private String generatePrefix() {
    return RandomStringUtils.randomAlphanumeric(7);
  }

  private String generateKey() {
    var uuid = UUID.randomUUID();
    return Base64.getEncoder().withoutPadding().encodeToString(uuid.toString().getBytes());
  }

  private String hash(String key) {
    return passwordEncoder.encode(key);
  }
}
