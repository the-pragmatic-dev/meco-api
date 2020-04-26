# MECO API: Email Service

## Outline

The Email Service utilises the [Mailgun](https://www.mailgun.com/) API and presents a single control point to send transactional emails programmatically while using the MECO API service. 

> You will need to verify an email in the Mailgun dashboard to enable outbound emails from the sandbox environment.

### Core Actions

Templates have been created in Mailgun for each of the given scenarios below. Some of these templates are parameterised. For these templates, we pass a `MultiValueMap` to Mailgun's API containing our parameters.

#### Account service

- Send a welcome email when a new account is created.
- Send a forgotten password email to a user with reset token.
- Send an email informing the user their password has been updated.
- Send an email informing the user that an unrecognized device signed in to the account.

#### KMS service

- Send an email informing the user a new key has been created.
- Send an email informing the user a key has been deleted.

#### Billing service

TODO