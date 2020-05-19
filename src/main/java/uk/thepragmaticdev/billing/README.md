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
  -d description='Starter product description...' \
  -d name='Starter' \
  -d statement_descriptor='TPD LTD MECO' \
  -d type='service' \
  -d unit_label='operation'

# Growth
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_growth' \
  -d active=true \
  -d description='Growth product description...' \
  -d name='Growth' \
  -d statement_descriptor='TPD LTD MECO' \
  -d type='service' \
  -d unit_label='operation'

# Pro
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_pro' \
  -d active=true \
  -d description='Pro product description...' \
  -d description='Starter product...' \
  -d name='Pro' \
  -d statement_descriptor='TPD LTD MECO' \
  -d type='service' \
  -d unit_label='operation'
```

### Create monthly and yearly prices for each product

```bash
# Starter
curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='Starter Price Monthly' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=month \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=5000 \
  -d 'tiers[0][up_to]'=10000 \
  -d 'tiers[1][unit_amount_decimal]'=0.2 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_starter

curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='Starter Price Yearly' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=year \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=54000 \
  -d 'tiers[0][up_to]'=120000 \
  -d 'tiers[1][unit_amount_decimal]'=0.2 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_starter

# Growth
curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='Growth Price Monthly' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=month \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=20000 \
  -d 'tiers[0][up_to]'=100000 \
  -d 'tiers[1][unit_amount_decimal]'=0.1 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_growth

curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='Growth Price Yearly' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=year \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=216000 \
  -d 'tiers[0][up_to]'=1200000 \
  -d 'tiers[1][unit_amount_decimal]'=0.1 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_growth

# Pro
curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='Pro Price Monthly' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=month \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=50000 \
  -d 'tiers[0][up_to]'=500000 \
  -d 'tiers[1][unit_amount_decimal]'=0.05 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_pro

curl https://api.stripe.com/v1/prices \
  -u {{API_KEY}} \
  -d billing_scheme=tiered \
  -d currency=gbp \
  -d nickname='Pro Price Yearly' \
  -d 'recurring[aggregate_usage]'=sum \
  -d 'recurring[interval]'=month \
  -d 'recurring[interval_count]'=1 \
  -d 'recurring[usage_type]'=metered \
  -d tiers_mode=graduated \
  -d 'tiers[0][flat_amount]'=540000 \
  -d 'tiers[0][up_to]'=6000000 \
  -d 'tiers[1][unit_amount_decimal]'=0.05 \
  -d 'tiers[1][up_to]'=inf \
  -d product=prod_pro
```
