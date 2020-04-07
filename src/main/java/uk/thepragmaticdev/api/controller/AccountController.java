package uk.thepragmaticdev.api.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
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
   * TODO tested.
   * 
   * @param principal TODO
   * @return
   */
  @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
  public Account findAuthenticatedAccount(Principal principal) {
    return accountService.findAuthenticatedAccount(principal.getName());
  }

  /**
   * TODO tested.
   * 
   * @param principal TODO
   * @param account   TODO
   * @return
   */
  @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Account update(Principal principal, @Valid @RequestBody Account account) {
    return accountService.update(principal.getName(), account);
  }

  /**
   * TODO tested.
   * 
   * @param account TODO
   * @return
   */
  @PostMapping(value = "/signin", consumes = MediaType.APPLICATION_JSON_VALUE)
  public String signin(@Valid @RequestBody Account account) {
    return accountService.signin(account.getUsername(), account.getPassword());
  }

  /**
   * TODO tested.
   * 
   * @param account TODO
   * @return
   */
  @ResponseStatus(value = HttpStatus.CREATED)
  @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
  public String signup(@Valid @RequestBody Account account) {
    return accountService.signup(account);
  }

  /**
   * TODO.
   * 
   * @param pageable  TODO
   * @param principal TODO
   * @return
   */
  @GetMapping(value = "/me/billing/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<BillingLog> billingLogs(Pageable pageable, Principal principal) {
    return accountService.billingLogs(pageable, principal.getName());
  }

  /**
   * TODO.
   * 
   * @param response  TODO
   * @param principal TODO
   */
  @GetMapping(value = "/me/billing/logs/download")
  public void downloadBillingLogs(HttpServletResponse response, Principal principal) {
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "billing.csv" + "\"");
    accountService.downloadBillingLogs(response, principal.getName());
  }

  /**
   * TODO.
   * 
   * @param pageable  TODO
   * @param principal TODO
   * @return
   */
  @GetMapping(value = "/me/security/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  public Page<SecurityLog> securityLogs(Pageable pageable, Principal principal) {
    return accountService.securityLogs(pageable, principal.getName());
  }

  /**
   * TODO.
   * 
   * @param response  TODO
   * @param principal TODO
   */
  @GetMapping(value = "/me/security/logs/download")
  public void downloadSecurityLogs(HttpServletResponse response, Principal principal) {
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Access-Control-Expose-Headers", HttpHeaders.CONTENT_DISPOSITION);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "security.csv" + "\"");
    accountService.downloadSecurityLogs(response, principal.getName());
  }
}