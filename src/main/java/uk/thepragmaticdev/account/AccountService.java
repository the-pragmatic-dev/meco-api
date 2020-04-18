package uk.thepragmaticdev.account;

import com.opencsv.ICSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AccountCode;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.billing.BillingLogService;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.log.security.SecurityLogService;
import uk.thepragmaticdev.security.JwtTokenProvider;

@Service
public class AccountService {

  private AccountRepository accountRepository;

  private BillingLogService billingLogService;

  private SecurityLogService securityLogService;

  private EmailService emailService;

  private PasswordEncoder passwordEncoder;

  private JwtTokenProvider jwtTokenProvider;

  private AuthenticationManager authenticationManager;

  /**
   * Service for creating, authorizing and updating accounts. Billing and security
   * logs related to an authorised account may also be downloaded.
   * 
   * @param accountRepository     The data access repository for accounts
   * @param billingLogService     The service for accessing billing logs
   * @param securityLogService    The service for accessing security logs
   * @param emailService          The service for sending emails
   * @param passwordEncoder       The service for encoding passwords
   * @param jwtTokenProvider      The provider for creating, validating tokens
   * @param authenticationManager The manager for authentication providers
   */
  @Autowired
  public AccountService(//
      AccountRepository accountRepository, //
      BillingLogService billingLogService, //
      SecurityLogService securityLogService, //
      EmailService emailService, //
      PasswordEncoder passwordEncoder, //
      JwtTokenProvider jwtTokenProvider, //
      AuthenticationManager authenticationManager) {
    this.accountRepository = accountRepository;
    this.billingLogService = billingLogService;
    this.securityLogService = securityLogService;
    this.emailService = emailService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.authenticationManager = authenticationManager;
  }

  /**
   * Authorize an account.
   * 
   * @param username The username of an account attemping to sign in
   * @param password The password of an account attemping to sign in
   * @return An authentication token
   */
  public String signin(String username, String password) {
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
      return jwtTokenProvider.createToken(username, findAuthenticatedAccount(username).getRoles());
    } catch (AuthenticationException e) {
      throw new ApiException(AccountCode.INVALID_CREDENTIALS);
    }
  }

  /**
   * Create a new account.
   * 
   * @param account The new account to be created
   * @return A newly created account
   */
  public String signup(Account account) {
    if (!accountRepository.existsByUsername(account.getUsername())) {
      account.setPassword(passwordEncoder.encode(account.getPassword()));
      account.setRoles(Arrays.asList(Role.ROLE_ADMIN));
      account.setCreatedDate(OffsetDateTime.now());
      Account persistedAccount = accountRepository.save(account);
      securityLogService.created(persistedAccount.getId());
      emailService.sendAccountCreated(persistedAccount);
      return jwtTokenProvider.createToken(persistedAccount.getUsername(), persistedAccount.getRoles());
    } else {
      throw new ApiException(AccountCode.USERNAME_UNAVAILABLE);
    }
  }

  /**
   * Find the currently authenticated account.
   * 
   * @param username The authenticated account username
   * @return The authenticated account
   */
  public Account findAuthenticatedAccount(String username) {
    return accountRepository.findByUsername(username)
        .orElseThrow(() -> new ApiException(AccountCode.USERNAME_NOT_FOUND));
  }

  /**
   * Update all mutable fields of an authenticated account if a change is
   * detected.
   * 
   * @param username The authenticated account username
   * @param account  An account with the desired values
   * @return The updated account
   */
  public Account update(String username, @Valid Account account) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    updateFullName(authenticatedAccount, account.getFullName());
    updateEmailSubscriptionEnabled(authenticatedAccount, account.getEmailSubscriptionEnabled());
    updateBillingAlertEnabled(authenticatedAccount, account.getBillingAlertEnabled());
    return accountRepository.save(authenticatedAccount);
  }

  private void updateFullName(Account authenticatedAccount, String fullName) {
    if (authenticatedAccount.getFullName() == null ? fullName != null
        : !authenticatedAccount.getFullName().equals(fullName)) {
      securityLogService.fullname(authenticatedAccount.getId());
      authenticatedAccount.setFullName(fullName);
    }
  }

  private void updateBillingAlertEnabled(Account authenticatedAccount, boolean billingAlertEnabled) {
    if (authenticatedAccount.getBillingAlertEnabled() != billingAlertEnabled) {
      securityLogService.billingAlertEnabled(authenticatedAccount.getId(), billingAlertEnabled);
      authenticatedAccount.setBillingAlertEnabled(billingAlertEnabled);
    }
  }

  private void updateEmailSubscriptionEnabled(Account authenticatedAccount, boolean emailSubscriptionEnabled) {
    if (authenticatedAccount.getEmailSubscriptionEnabled() != emailSubscriptionEnabled) {
      securityLogService.emailSubscriptionEnabled(authenticatedAccount.getId(), emailSubscriptionEnabled);
      authenticatedAccount.setEmailSubscriptionEnabled(emailSubscriptionEnabled);
    }
  }

  /**
   * Send a forgotten password email to the requested account.
   * 
   * @param username A valid account username
   */
  public void forgot(String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    authenticatedAccount.setPasswordResetToken(UUID.randomUUID().toString());
    accountRepository.save(authenticatedAccount);
    emailService.sendForgottenPassword(authenticatedAccount);
  }

  /**
   * Reset old password to new password for the requested account.
   * 
   * @param account An account containing the new password
   * @param token   The generated password reset token from the /me/forgot
   *                endpoint
   */
  public void reset(Account account, String token) {
    Account persistedAccount = accountRepository.findByPasswordResetToken(token)
        .orElseThrow(() -> new ApiException(AccountCode.INVALID_PASSWORD_RESET_TOKEN));
    persistedAccount.setPassword(passwordEncoder.encode(account.getPassword()));
    persistedAccount.setPasswordResetToken(null);
    accountRepository.save(persistedAccount);
    securityLogService.reset(persistedAccount.getId());
  }

  /**
   * Find the latest billing logs for the account.
   * 
   * @param pageable The pagination information
   * @param username The authenticated account username
   * @return A page of the latest billing logs
   */
  public Page<BillingLog> billingLogs(Pageable pageable, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    return billingLogService.findAllByAccountId(pageable, authenticatedAccount.getId());
  }

  /**
   * Download the latest billing logs for the account as a CSV file.
   * 
   * @param response The servlet response
   * @param username The authenticated account username
   */
  public void downloadBillingLogs(HttpServletResponse response, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    try {
      StatefulBeanToCsv<BillingLog> writer = new StatefulBeanToCsvBuilder<BillingLog>(response.getWriter())
          .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER).withSeparator(ICSVWriter.DEFAULT_SEPARATOR)
          .withOrderedResults(true).build();
      writer.write(billingLogService.findAllByAccountId(authenticatedAccount.getId()));
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
      throw new ApiException(CriticalCode.CSV_WRITING_ERROR);
    } catch (IOException e) {
      throw new ApiException(CriticalCode.PRINT_WRITER_IO_ERROR);
    }
  }

  /**
   * Find the latest security logs for the account.
   * 
   * @param pageable The pagination information
   * @param username The authenticated account username
   * @return A page of the latest security logs
   */
  public Page<SecurityLog> securityLogs(Pageable pageable, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    return securityLogService.findAllByAccountId(pageable, authenticatedAccount.getId());
  }

  /**
   * Download the latest security logs for the account as a CSV file.
   * 
   * @param response The servlet response
   * @param username The authenticated account username
   */
  public void downloadSecurityLogs(HttpServletResponse response, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    try {
      StatefulBeanToCsv<SecurityLog> writer = new StatefulBeanToCsvBuilder<SecurityLog>(response.getWriter())
          .withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER).withSeparator(ICSVWriter.DEFAULT_SEPARATOR)
          .withOrderedResults(true).build();
      writer.write(securityLogService.findAllByAccountId(authenticatedAccount.getId()));
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
      throw new ApiException(CriticalCode.CSV_WRITING_ERROR);
    } catch (IOException e) {
      throw new ApiException(CriticalCode.PRINT_WRITER_IO_ERROR);
    }
  }

  @SuppressWarnings("unused")
  private String refresh(String username) {
    return jwtTokenProvider.createToken(username, findAuthenticatedAccount(username).getRoles());
  }
}
