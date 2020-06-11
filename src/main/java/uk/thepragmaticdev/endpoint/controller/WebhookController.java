package uk.thepragmaticdev.endpoint.controller;

import com.stripe.model.Event;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/webhooks")
@CrossOrigin("*")
public class WebhookController {

  @PostMapping
  public String handleWebhookEvent(Event event) {
    // TODO: Handle Stripe webhooks.
    return "homepage";
  }
}