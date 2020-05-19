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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.billing.dto.request.BillingCreateSubscriptionRequest;
import uk.thepragmaticdev.billing.dto.response.BillingPriceResponse;

@RestController
@RequestMapping("/billing")
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
   * Find all active prices held by stripe.
   * 
   * @return A list of all active prices held by stripe
   */
  @GetMapping(value = "/prices", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.OK)
  public List<BillingPriceResponse> findAllPrices() {
    var prices = billingService.findAllPrices().getData();
    return prices.stream().map(price -> modelMapper.map(price, BillingPriceResponse.class))
        .collect(Collectors.toList());
  }

  /**
   * Create a new stripe subscription for the given customer id to the given price
   * id.
   * 
   * @param principal The currently authenticated principal user
   * @param request   A new subscription request with the desired price
   */
  @PostMapping(value = "/subscriptions", consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.CREATED)
  public void createSubscription(Principal principal, @Valid @RequestBody BillingCreateSubscriptionRequest request) {
    billingService.createSubscription(principal.getName(), request.getPrice());
  }

  /**
   * Cancel an active stripe subscription.
   * 
   * @param principal The currently authenticated principal user
   */
  @DeleteMapping("/subscriptions")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void cancelSubscription(Principal principal) {
    billingService.cancelSubscription(principal.getName());
  }
}