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

  private long operations;

  private long amount;

  private double unitAmount;

  /**
   * An invoice line wrapper around a stripe invoice line item. The description is
   * dymanic based on the type of line item.
   * 
   * @param invoiceLineItem A stripe invoice line item
   */
  public InvoiceLineItemResponse(InvoiceLineItem invoiceLineItem) {
    // TODO descriptions from stripe too flaky needs changed
    if (invoiceLineItem.getAmount() < 0) {
      this.description = invoiceLineItem.getDescription();
      this.unitAmount = invoiceLineItem.getPrice().getUnitAmountDecimal().doubleValue();
    } else if (invoiceLineItem.getDescription().contains("Tier 1 at £0")) {
      if (invoiceLineItem.getPlan().getNickname().equals("starter")) {
        this.description = "First 1000";
      } else {
        this.description = String.format("First %d", invoiceLineItem.getPlan().getTiers().get(0).getUpTo());
      }
    } else if (invoiceLineItem.getDescription().contains("Tier 1 at £")) {
      this.description = String.format("Flat fee for first %d", invoiceLineItem.getPlan().getTiers().get(0).getUpTo());
    } else {
      this.description = String.format("%d and above", invoiceLineItem.getPlan().getTiers().get(0).getUpTo() + 1);
      this.unitAmount = invoiceLineItem.getPlan().getTiers().get(1).getUnitAmountDecimal().doubleValue();
    }
    this.operations = invoiceLineItem.getQuantity();
    this.amount = invoiceLineItem.getAmount();
  }
}
