# MECO API: Billing Service

## Outline

Subscriptions are managed through [Stripe](https://stripe.com/gb) and consist of a flat monthly rate with extra charges for usage that exceeds a fixed quota of operations. When a user signs up with a new account, we create a Stripe customer and save this to the account table. When a user subscribes to a paid plan, the subscription id and subscription item id are also stored alongside this. The following products and prices will need to be created in Stripe using your secret key:

### Create products

```bash
# Starter
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_starter' \
  -d active=true \
  -d name='Starter' \
  -d statement_descriptor='TPD LTD MECO' \
  -d type='service' \
  -d unit_label='operation'

# Indie
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_indie' \
  -d active=true \
  -d name='Indie' \
  -d statement_descriptor='TPD LTD MECO' \
  -d type='service' \
  -d unit_label='operation'

# Pro
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_pro' \
  -d active=true \
  -d name='Pro' \
  -d statement_descriptor='TPD LTD MECO' \
  -d type='service' \
  -d unit_label='operation'
```

### Create monthly prices for each product

```bash
# Starter
curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d currency=gbp \
  -d unit_amount=0 \
  -d nickname='starter' \
  -d 'recurring[interval]'=month \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d product=prod_starter

# Indie
curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='indie' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=month \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=5000 \
  -d 'tiers[0][up_to]'=10000 \
  -d 'tiers[1][unit_amount_decimal]'=0.2 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_indie

# Pro
curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='pro' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=month \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=20000 \
  -d 'tiers[0][up_to]'=100000 \
  -d 'tiers[1][unit_amount_decimal]'=0.1 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_pro
```
