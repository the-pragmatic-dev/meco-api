package uk.thepragmaticdev.billing.dto.response;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingResponse {

  private String customerId;

  private String subscriptionId;

  private String subscriptionItemId;

  private String subscriptionStatus;

  private OffsetDateTime subscriptionCurrentPeriodStart;

  private OffsetDateTime subscriptionCurrentPeriodEnd;

  private String planId;

  private String planNickname;

  private String cardBillingName;

  private String cardBrand;

  private String cardLast4;

  private Short cardExpMonth;

  private Short cardExpYear;

  private OffsetDateTime createdDate;

  private OffsetDateTime updatedDate;
}
