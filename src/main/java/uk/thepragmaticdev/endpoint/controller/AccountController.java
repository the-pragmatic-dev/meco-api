package uk.thepragmaticdev.endpoint.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.security.SecurityLog;

@RestController
@RequestMapping("/accounts")
@CrossOrigin("*")
@Tag(name = "accounts")
public class AccountController {

  private AccountService accountService;

  @Autowired
  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  /**
   * Authorize an account.
   * 
   * @param account The account attempting to sign
   * @return An authentication token
   */
  @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_JSON_VALUE)
  public String signin(@Valid @RequestBody Account account) {
    return accountService.signin(account.getUsername(), account.getPassword());
  }

  /**
   * Create a new account.
   * 
   * @param account The new account to be created
   * @return A newly created account
   */
  @ResponseStatus(value = HttpStatus.CREATED)
  @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
  public String signup(@Valid @RequestBody Account account) {
    return accountService.signup(account);
  }

  /**
   * Find the currently authenticated account.
   * 
   * @param principal The currently authenticated principal user
   * @return The authenticated account
   */
  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public Account findAuthenticatedAccount(Principal principal) {
    return accountService.findAuthenticatedAccount(principal.getName());
  }

  /**
   * Update all mutable fields of an authenticated account.
   * 
   * @param principal The currently authenticated principal user
   * @param account   An account with the desired values
   * @return The updated account
   */
  @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Account update(Principal principal, @Valid @RequestBody Account account) {
    return accountService.update(principal.getName(), account);
  }

  /**
   * Find the latest billing logs for the account.
   * 
   * @param pageable  The pagination information
   * @param principal The currently authenticated principal user
   * @return A page of the latest billing logs
   */
  @GetMapping(value = "/me/billing/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<BillingLog> billingLogs(Pageable pageable, Principal principal) {
    return accountService.billingLogs(pageable, principal.getName());
  }

  /**
   * Download the latest billing logs for the account as a CSV file.
   * 
   * @param response  The servlet response
   * @param principal The currently authenticated principal user
   */
  @GetMapping(value = "/me/billing/logs/download")
  public void downloadBillingLogs(HttpServletResponse response, Principal principal) {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "billing.csv" + "\"");
    accountService.downloadBillingLogs(response, principal.getName());
  }

  /**
   * Find the latest security logs for the account.
   * 
   * @param pageable  The pagination information
   * @param principal The currently authenticated principal user
   * @return A page of the latest security logs
   */
  @GetMapping(value = "/me/security/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<SecurityLog> securityLogs(Pageable pageable, Principal principal) {
    return accountService.securityLogs(pageable, principal.getName());
  }

  /**
   * Download the latest security logs for the account as a CSV file.
   * 
   * @param response  The servlet response
   * @param principal The currently authenticated principal user
   */
  @GetMapping(value = "/me/security/logs/download")
  public void downloadSecurityLogs(HttpServletResponse response, Principal principal) {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "security.csv" + "\"");
    accountService.downloadSecurityLogs(response, principal.getName());
  }
}