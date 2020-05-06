package uk.thepragmaticdev.log.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BillingLogResponse {

  private String action;

  private OffsetDateTime createdDate;

  private String amount;
}