package uk.thepragmaticdev.account;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
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
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.AccountCode;
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

  private PasswordEncoder passwordEncoder;

  private JwtTokenProvider jwtTokenProvider;

  private AuthenticationManager authenticationManager;

  /**
   * TODO.
   * 
   * @param accountRepository     TODO
   * @param billingLogService     TODO
   * @param securityLogService    TODO
   * @param passwordEncoder       TODO
   * @param jwtTokenProvider      TODO
   * @param authenticationManager TODO
   */
  @Autowired
  public AccountService(//
      AccountRepository accountRepository, //
      BillingLogService billingLogService, //
      SecurityLogService securityLogService, //
      PasswordEncoder passwordEncoder, //
      JwtTokenProvider jwtTokenProvider, //
      AuthenticationManager authenticationManager) {
    this.accountRepository = accountRepository;
    this.billingLogService = billingLogService;
    this.securityLogService = securityLogService;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.authenticationManager = authenticationManager;
  }

  /**
   * TODO.
   * 
   * @param username TODO
   * @return
   */
  public Account findAuthenticatedAccount(String username) {
    return accountRepository.findByUsername(username)
        .orElseThrow(() -> new ApiException(AccountCode.USERNAME_NOT_FOUND));
  }

  /**
   * TODO.
   * 
   * @param username TODO
   * @param account  TODO
   * @return
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
   * TODO.
   * 
   * @param username TODO
   * @param password TODO
   * @return
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
   * TODO.
   * 
   * @param account TODO
   * @return
   */
  public String signup(Account account) {
    if (!accountRepository.existsByUsername(account.getUsername())) {
      account.setPassword(passwordEncoder.encode(account.getPassword()));
      account.setRoles(Arrays.asList(Role.ROLE_ADMIN));
      account.setCreatedDate(OffsetDateTime.now());
      accountRepository.save(account);
      securityLogService.created(account.getId());
      return jwtTokenProvider.createToken(account.getUsername(), account.getRoles());
    } else {
      throw new ApiException(AccountCode.USERNAME_UNAVAILABLE);
    }
  }

  /**
   * TODO.
   * 
   * @param pageable TODO
   * @param username TODO
   * @return
   */
  public Page<BillingLog> billingLogs(Pageable pageable, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    return billingLogService.findAllByAccountId(pageable, authenticatedAccount.getId());
  }

  /**
   * TODO.
   * 
   * @param response TODO
   * @param username TODO
   */
  public void downloadBillingLogs(HttpServletResponse response, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    try {
      StatefulBeanToCsv<BillingLog> writer = new StatefulBeanToCsvBuilder<BillingLog>(response.getWriter())
          .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER).withSeparator(CSVWriter.DEFAULT_SEPARATOR)
          .withOrderedResults(true).build();
      writer.write(billingLogService.findAllByAccountId(authenticatedAccount.getId()));
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
      // TODO Auto-generated catch block
    } catch (IOException e) {
      // TODO Auto-generated catch block
    }
  }

  /**
   * TODO.
   * 
   * @param pageable TODO
   * @param username TODO
   * @return
   */
  public Page<SecurityLog> securityLogs(Pageable pageable, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    return securityLogService.findAllByAccountId(pageable, authenticatedAccount.getId());
  }

  /**
   * TODO.
   * 
   * @param response TODO
   * @param username TODO
   */
  public void downloadSecurityLogs(HttpServletResponse response, String username) {
    Account authenticatedAccount = findAuthenticatedAccount(username);
    try {
      StatefulBeanToCsv<SecurityLog> writer = new StatefulBeanToCsvBuilder<SecurityLog>(response.getWriter())
          .withQuotechar(CSVWriter.NO_QUOTE_CHARACTER).withSeparator(CSVWriter.DEFAULT_SEPARATOR)
          .withOrderedResults(true).build();
      writer.write(securityLogService.findAllByAccountId(authenticatedAccount.getId()));
    } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
      // TODO Auto-generated catch block
    } catch (IOException e) {
      // TODO Auto-generated catch block
    }
  }

  @SuppressWarnings("unused")
  private String refresh(String username) {
    return jwtTokenProvider.createToken(username, findAuthenticatedAccount(username).getRoles());
  }
}
