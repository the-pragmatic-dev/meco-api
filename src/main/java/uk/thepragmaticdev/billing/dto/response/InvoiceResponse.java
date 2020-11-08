package uk.thepragmaticdev.billing.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

  private String number;

  private String currency;

  private long subtotal;

  private long total;

  private long amountDue;

  private OffsetDateTime periodStart;

  private OffsetDateTime periodEnd;

  private List<InvoiceLineItemResponse> items;
}
