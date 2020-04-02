package uk.thepragmaticdev.api.controller;

import com.stripe.model.Event;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
@CrossOrigin("*")
@Tag(name = "webhooks")
public class WebhookController {

  public WebhookController() {
  }

  @PostMapping
  public String handleWebhookEvent(Event event) {
    // TODO: Handle Stripe webhooks.
    return "homepage";
  }
}