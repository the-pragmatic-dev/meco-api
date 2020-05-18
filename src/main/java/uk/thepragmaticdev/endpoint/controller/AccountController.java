package uk.thepragmaticdev.endpoint.controller;

import com.opencsv.bean.StatefulBeanToCsv;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.account.dto.request.AccountResetRequest;
import uk.thepragmaticdev.account.dto.request.AccountSigninRequest;
import uk.thepragmaticdev.account.dto.request.AccountSignupRequest;
import uk.thepragmaticdev.account.dto.request.AccountUpdateRequest;
import uk.thepragmaticdev.account.dto.response.AccountMeResponse;
import uk.thepragmaticdev.account.dto.response.AccountSigninResponse;
import uk.thepragmaticdev.account.dto.response.AccountSignupResponse;
import uk.thepragmaticdev.account.dto.response.AccountUpdateResponse;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.dto.BillingLogResponse;
import uk.thepragmaticdev.log.dto.SecurityLogResponse;
import uk.thepragmaticdev.log.security.SecurityLog;

@RestController
@RequestMapping("/accounts")
@CrossOrigin("*")
@Tag(name = "accounts")
public class AccountController {

  private final AccountService accountService;

  private final ModelMapper modelMapper;

  private final StatefulBeanToCsv<BillingLog> billingLogWriter;

  private final StatefulBeanToCsv<SecurityLog> securityLogWriter;

  /**
   * Endpoint for accounts.
   * 
   * @param accountService    The service for retrieving account information
   * @param modelMapper       An entity to domain mapper
   * @param billingLogWriter  A csv writer for billing logs
   * @param securityLogWriter A csv writer for billing logs
   */
  @Autowired
  public AccountController(AccountService accountService, ModelMapper modelMapper,
      StatefulBeanToCsv<BillingLog> billingLogWriter, StatefulBeanToCsv<SecurityLog> securityLogWriter) {
    this.accountService = accountService;
    this.modelMapper = modelMapper;
    this.billingLogWriter = billingLogWriter;
    this.securityLogWriter = securityLogWriter;
  }

  /**
   * Authorize an account.
   * 
   * @param request The request information for HTTP servlets
   * @param signin  The account details for signing in
   * @return An authentication token
   */
  @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AccountSigninResponse signin(HttpServletRequest request, @Valid @RequestBody AccountSigninRequest signin) {
    var token = accountService.signin(signin.getUsername(), signin.getPassword(), request);
    return new AccountSigninResponse(token);
  }

  /**
   * Create a new account.
   * 
   * @param request The new account details to be created
   * @return A newly created account
   */
  @ResponseStatus(value = HttpStatus.CREATED)
  @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AccountSignupResponse signup(@Valid @RequestBody AccountSignupRequest request) {
    var token = accountService.signup(request.getUsername(), request.getPassword());
    return new AccountSignupResponse(token);
  }

  /**
   * Find the currently authenticated account.
   * 
   * @param principal The currently authenticated principal user
   * @return The authenticated account
   */
  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public AccountMeResponse findAuthenticatedAccount(Principal principal) {
    var account = accountService.findAuthenticatedAccount(principal.getName());
    return modelMapper.map(account, AccountMeResponse.class);
  }

  /**
   * Update all mutable fields of an authenticated account.
   * 
   * @param principal The currently authenticated principal user
   * @param request   Details of account with the desired values
   * @return The updated account
   */
  @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AccountUpdateResponse update(Principal principal, @Valid @RequestBody AccountUpdateRequest request) {
    var account = accountService.update(principal.getName(), request.getFullName(),
        request.getEmailSubscriptionEnabled(), request.getBillingAlertEnabled());
    return modelMapper.map(account, AccountUpdateResponse.class);
  }

  /**
   * Send a forgotten password email to the requested account.
   * 
   * @param username A valid account username
   */
  @PostMapping(value = "/me/forgot")
  public void forgot(@RequestParam(value = "username", required = true) String username) {
    accountService.forgot(username);
  }

  /**
   * Reset old password to new password for the requested account.
   * 
   * @param request An request containing the new password
   * @param token   The generated password reset token from the /me/forgot
   *                endpoint
   */
  @PostMapping(value = "/me/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void reset(@Valid @RequestBody AccountResetRequest request,
      @RequestParam(value = "token", required = true) String token) {
    accountService.reset(request.getPassword(), token);
  }

  /**
   * Find the latest billing logs for the account.
   * 
   * @param pageable  The pagination information
   * @param principal The currently authenticated principal user
   * @return A page of the latest billing logs
   */
  @GetMapping(value = "/me/billing/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<BillingLogResponse> billingLogs(Pageable pageable, Principal principal) {
    var billingLogs = accountService.billingLogs(pageable, principal.getName());
    return billingLogs.map(log -> new BillingLogResponse(log.getAction(), log.getCreatedDate(), log.getAmount()));
  }

  /**
   * Download all billing logs for the account as a CSV file.
   * 
   * @param principal The currently authenticated principal user
   */
  @GetMapping(value = "/me/billing/logs/download", produces = "text/csv")
  public void downloadBillingLogs(Principal principal) {
    accountService.downloadBillingLogs(billingLogWriter, principal.getName());
  }

  /**
   * Find the latest security logs for the account.
   * 
   * @param pageable  The pagination information
   * @param principal The currently authenticated principal user
   * @return A page of the latest security logs
   */
  @GetMapping(value = "/me/security/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<SecurityLogResponse> securityLogs(Pageable pageable, Principal principal) {
    var securityLogs = accountService.securityLogs(pageable, principal.getName());
    return securityLogs
        .map(log -> new SecurityLogResponse(log.getAction(), log.getCreatedDate(), log.getRequestMetadata()));
  }

  /**
   * Download all security logs for the account as a CSV file.
   * 
   * @param principal The currently authenticated principal user
   */
  @GetMapping(value = "/me/security/logs/download", produces = "text/csv")
  public void downloadSecurityLogs(Principal principal) {
    accountService.downloadSecurityLogs(securityLogWriter, principal.getName());
  }
}