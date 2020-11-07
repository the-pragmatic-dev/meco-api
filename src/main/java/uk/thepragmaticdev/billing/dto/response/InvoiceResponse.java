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

  public String number;

  public String currency;

  public long subtotal;

  public long total;

  public long amountDue;

  public OffsetDateTime periodStart;

  public OffsetDateTime periodEnd;

  public List<InvoiceLineItemResponse> items;
}
