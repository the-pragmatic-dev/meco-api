# MECO API: Billing Service

## Outline

### Create products

```bash
# Starter
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_starter' \
  -d active=true \
  -d name='Starter' \
  -d type='service' \
  -d unit_label='operation'

# Growth
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_growth' \
  -d active=true \
  -d name='Growth' \
  -d type='service' \
  -d unit_label='operation'

# Pro
curl https://api.stripe.com/v1/products \
  -u {{API_KEY}} \
  -d id='prod_pro' \
  -d active=true \
  -d name='Pro' \
  -d type='service' \
  -d unit_label='operation'
```

### Create monthly and yearly plans for each product

```bash
# Starter
curl https://api.stripe.com/v1/plans \
  -u {{API_KEY}} \
  -d id='plan_starter_monthly' \
  -d active=true \
  -d aggregate_usage='sum' \
  -d billing_scheme='tiered' \
  -d currency='gbp' \
  -d interval='month' \
  -d interval_count=1 \
  -d nickname='Monthly' \
  -d product='prod_starter' \
  -d 'tiers[0][flat_amount]'=5000 \
  -d 'tiers[0][unit_amount]'=0 \
  -d 'tiers[0][up_to]'=10000 \
  -d 'tiers[1][unit_amount_decimal]'=0.2 \
  -d 'tiers[1][up_to]'=inf \
  -d tiers_mode='graduated' \
  -d usage_type='metered'

curl https://api.stripe.com/v1/plans \
  -u {{API_KEY}} \
  -d id='plan_starter_yearly' \
  -d active=true \
  -d aggregate_usage='sum' \
  -d billing_scheme='tiered' \
  -d currency='gbp' \
  -d interval='year' \
  -d interval_count=1 \
  -d nickname='Yearly' \
  -d product='prod_starter' \
  -d 'tiers[0][flat_amount]'=54000 \
  -d 'tiers[0][unit_amount]'=0 \
  -d 'tiers[0][up_to]'=120000 \
  -d 'tiers[1][unit_amount_decimal]'=0.2 \
  -d 'tiers[1][up_to]'=inf \
  -d tiers_mode='graduated' \
  -d usage_type='metered'

# Growth
curl https://api.stripe.com/v1/plans \
  -u {{API_KEY}} \
  -d id='plan_growth_monthly' \
  -d active=true \
  -d aggregate_usage='sum' \
  -d billing_scheme='tiered' \
  -d currency='gbp' \
  -d interval='month' \
  -d interval_count=1 \
  -d nickname='Monthly' \
  -d product='prod_growth' \
  -d 'tiers[0][flat_amount]'=20000 \
  -d 'tiers[0][unit_amount]'=0 \
  -d 'tiers[0][up_to]'=100000 \
  -d 'tiers[1][unit_amount_decimal]'=0.1 \
  -d 'tiers[1][up_to]'=inf \
  -d tiers_mode='graduated' \
  -d usage_type='metered'

curl https://api.stripe.com/v1/plans \
  -u {{API_KEY}} \
  -d id='plan_growth_yearly' \
  -d active=true \
  -d aggregate_usage='sum' \
  -d billing_scheme='tiered' \
  -d currency='gbp' \
  -d interval='year' \
  -d interval_count=1 \
  -d nickname='Yearly' \
  -d product='prod_growth' \
  -d 'tiers[0][flat_amount]'=216000 \
  -d 'tiers[0][unit_amount]'=0 \
  -d 'tiers[0][up_to]'=1200000 \
  -d 'tiers[1][unit_amount_decimal]'=0.1 \
  -d 'tiers[1][up_to]'=inf \
  -d tiers_mode='graduated' \
  -d usage_type='metered'

# Pro
curl https://api.stripe.com/v1/plans \
  -u {{API_KEY}} \
  -d id='plan_pro_monthly' \
  -d active=true \
  -d aggregate_usage='sum' \
  -d billing_scheme='tiered' \
  -d currency='gbp' \
  -d interval='month' \
  -d interval_count=1 \
  -d nickname='Monthly' \
  -d product='prod_pro' \
  -d 'tiers[0][flat_amount]'=50000 \
  -d 'tiers[0][unit_amount]'=0 \
  -d 'tiers[0][up_to]'=500000 \
  -d 'tiers[1][unit_amount_decimal]'=0.05 \
  -d 'tiers[1][up_to]'=inf \
  -d tiers_mode='graduated' \
  -d usage_type='metered'

curl https://api.stripe.com/v1/plans \
  -u {{API_KEY}} \
  -d id='plan_pro_yearly' \
  -d active=true \
  -d aggregate_usage='sum' \
  -d billing_scheme='tiered' \
  -d currency='gbp' \
  -d interval='year' \
  -d interval_count=1 \
  -d nickname='Yearly' \
  -d product='prod_pro' \
  -d 'tiers[0][flat_amount]'=540000 \
  -d 'tiers[0][unit_amount]'=0 \
  -d 'tiers[0][up_to]'=6000000 \
  -d 'tiers[1][unit_amount_decimal]'=0.05 \
  -d 'tiers[1][up_to]'=inf \
  -d tiers_mode='graduated' \
  -d usage_type='metered'

```
