package uk.thepragmaticdev.account.scheduler;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.kms.usage.ApiKeyUsageService;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AccountScheduler {

  private final AccountService accountService;

  private final ApiKeyUsageService apiKeyUsageService;

  private final int freePlanLimit;

  /**
   * Singleton which periodically checks for accounts that have exceeded their
   * plan limits. If exceeded we freeze all API keys relating to the account.
   * 
   * @param accountService     The service for managing accounts.
   * @param apiKeyUsageService The service for finding api key usage.
   * @param freePlanLimit      Maximum number of operations permitted on free
   *                           plan.
   */
  public AccountScheduler(AccountService accountService, ApiKeyUsageService apiKeyUsageService,
      @Value("${billing.free-plan-limit}") int freePlanLimit) {
    this.accountService = accountService;
    this.apiKeyUsageService = apiKeyUsageService;
    this.freePlanLimit = freePlanLimit;
  }

  /**
   * Scheduled job to freeze accounts which either have no associated plan or have
   * exceeded their free plan limit.
   */
  @Transactional
  @Scheduled(fixedDelay = 30000)
  public void freeze() {
    var accounts = accountService.findAll();
    accounts.forEach(account -> {
      if (account.getBilling().getPlanNickname() == null) {
        accountService.freeze(account); // no active plan
        return;
      }
      if (account.getBilling().getSubscriptionCurrentPeriodEnd().isBefore(OffsetDateTime.now())) {
        accountService.freeze(account); // plan renewal past due date
        return;
      }
      if (account.getBilling().getPlanNickname().equals(BillingService.PLAN_STARTER)) {
        var ids = account.getApiKeys().stream().map(ApiKey::getId).collect(Collectors.toList());
        var usages = apiKeyUsageService.findByApiKeyInAndRange(ids,
            account.getBilling().getSubscriptionCurrentPeriodStart().toLocalDate(),
            account.getBilling().getSubscriptionCurrentPeriodEnd().toLocalDate());
        if (apiKeyUsageService.count(usages) >= freePlanLimit) {
          accountService.freeze(account); // free plan exceeded
        }
      }
    });
  }
}
