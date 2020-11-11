package uk.thepragmaticdev.endpoint.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.webhook.WebhookService;

@RestController
@RequestMapping("v1/webhooks")
@CrossOrigin("*")
public class WebhookController {

  private final WebhookService webhookService;

  /**
   * Endpoint for webhooks.
   * 
   * @param webhookService Service for managing webhook requests
   */
  @Autowired
  public WebhookController(WebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @PostMapping(value = "/stripe", consumes = MediaType.APPLICATION_JSON_VALUE)
  public void handleWebhookEvent(@RequestBody String payload, HttpServletRequest httpRequest) {
    webhookService.handleStripeEvent(payload, httpRequest.getHeader("stripe-signature"));
  }
}