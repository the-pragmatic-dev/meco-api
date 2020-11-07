package uk.thepragmaticdev.endpoint.controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.billing.dto.request.BillingCreateSubscriptionRequest;
import uk.thepragmaticdev.billing.dto.response.BillingPlanResponse;
import uk.thepragmaticdev.billing.dto.response.BillingResponse;
import uk.thepragmaticdev.billing.dto.response.InvoiceResponse;

@RestController
@RequestMapping("v1/billing")
@CrossOrigin("*")
public class BillingController {

  private final BillingService billingService;

  private final ModelMapper modelMapper;

  /**
   * Endpoint for billing.
   * 
   * @param billingService The service for handling payments
   * @param modelMapper    An entity to domain mapper
   */
  @Autowired
  public BillingController(BillingService billingService, ModelMapper modelMapper) {
    this.billingService = billingService;
    this.modelMapper = modelMapper;
  }

  /**
   * Create a new stripe customer and associate it with the given account.
   * 
   * @param principal The currently authenticated principal user
   * @return The customer billing information
   */
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.CREATED)
  public BillingResponse createCustomer(Principal principal) {
    var billing = billingService.createCustomer(principal.getName());
    return modelMapper.map(billing, BillingResponse.class);
  }

  /**
   * Find all active plans held by stripe.
   * 
   * @return A list of all active plans held by stripe
   */
  @GetMapping(value = "/plans", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<BillingPlanResponse> findAllPlans() {
    var plans = billingService.findAllPlans().getData();
    return plans.stream().map(plan -> modelMapper.map(plan, BillingPlanResponse.class)).collect(Collectors.toList());
  }

  /**
   * Create a new stripe subscription for the given customer id to the given price
   * id.
   * 
   * @param principal The currently authenticated principal user
   * @param request   A new subscription request with the desired price
   * @return The customer billing information
   */
  @PostMapping(value = "/subscriptions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.CREATED)
  public BillingResponse createSubscription(Principal principal,
      @Valid @RequestBody BillingCreateSubscriptionRequest request) {
    var billing = billingService.createSubscription(//
        principal.getName(), //
        request.getPaymentMethodId(), //
        request.getPlanId());
    return modelMapper.map(billing, BillingResponse.class);
  }

  /**
   * Update an existing stripe subscription for the given customer id to the given
   * price id.
   * 
   * @param principal The currently authenticated principal user
   * @param request   A new subscription request with the desired price
   * @return The customer billing information
   */
  @PutMapping(value = "/subscriptions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public BillingResponse updateSubscription(Principal principal,
      @Valid @RequestBody BillingCreateSubscriptionRequest request) {
    var billing = billingService.updateSubscription(//
        principal.getName(), //
        request.getPlanId());
    return modelMapper.map(billing, BillingResponse.class);
  }

  /**
   * Cancel an active stripe subscription.
   * 
   * @param principal The currently authenticated principal user
   * @return The customer billing information
   */
  @DeleteMapping(value = "/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
  public BillingResponse cancelSubscription(Principal principal) {
    var billing = billingService.cancelSubscription(principal.getName());
    return modelMapper.map(billing, BillingResponse.class);
  }

  /**
   * Find upcoming invoice for stripe customer.
   * 
   * @param principal The currently authenticated principal user
   * @return The upcoming invoice
   */
  @GetMapping(value = "/invoices/upcoming", produces = MediaType.APPLICATION_JSON_VALUE)
  public InvoiceResponse findUpcomingInvoice(Principal principal) {
    return billingService.findUpcomingInvoice(principal.getName());
  }
}