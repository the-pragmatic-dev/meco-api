package uk.thepragmaticdev.billing.dto.response;

import com.stripe.model.InvoiceLineItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLineItemResponse {

  private String description;

  private long amount;

  /**
   * An invoice line wrapper around a stripe invoice line item.
   * 
   * @param invoiceLineItem A stripe invoice line item
   */
  public InvoiceLineItemResponse(InvoiceLineItem invoiceLineItem) {
    this.description = invoiceLineItem.getDescription();
    this.amount = invoiceLineItem.getAmount();
  }
}
