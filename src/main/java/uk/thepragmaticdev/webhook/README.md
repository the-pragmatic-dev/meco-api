# MECO API: Webhook

## Outline

MECO listens to the following events from Stripe:

- invoice.payment_succeeded
- invoice.payment_failed
- customer.subscription.deleted

A customer in MECO is not able to delete a subscription, they may only downgrade to a free plan. Therefore if we see a `customer.subscription.deleted` event, this has been triggered automatically by Stripe after four failed payment attempts.

### Webhook testing

```bash
# generates a pair of secret API keys–one test mode, one live mode–that are valid for 90 days.
stripe login

# forward events to your server
stripe listen --forward-to localhost:8080/v1/webhooks/stripe

# trigger an event in new terminal
stripe trigger invoice.payment_succeeded
```
