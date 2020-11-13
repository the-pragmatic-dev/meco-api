-------------------------------------------------
-- Drop Tables ----------------------------------
DROP TABLE IF EXISTS refresh_token;
DROP TABLE IF EXISTS api_key_log;
DROP TABLE IF EXISTS security_log;
DROP TABLE IF EXISTS billing_log;
DROP TABLE IF EXISTS access_policy;
DROP TABLE IF EXISTS account_roles;
DROP TABLE IF EXISTS api_key_usage;
DROP TABLE IF EXISTS api_key;
DROP TABLE IF EXISTS scope;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS billing;
-------------------------------------------------
-- Drop Indexes ---------------------------------
DROP INDEX IF EXISTS refresh_token_token_idx;
DROP INDEX IF EXISTS api_key_log_api_key_id_idx;
DROP INDEX IF EXISTS security_log_account_id_idx;
DROP INDEX IF EXISTS billing_log_account_id_idx;
DROP INDEX IF EXISTS access_policy_api_key_id_idx;
DROP INDEX IF EXISTS account_roles_account_id_idx;
DROP INDEX IF EXISTS api_key_usage_usage_date_idx;
DROP INDEX IF EXISTS api_key_prefix_idx;
DROP INDEX IF EXISTS api_key_account_id_idx;
DROP INDEX IF EXISTS api_key_scope_id_idx;
DROP INDEX IF EXISTS account_username_idx;
DROP INDEX IF EXISTS account_password_reset_token_idx;
DROP INDEX IF EXISTS account_billing_id_idx;
DROP INDEX IF EXISTS billing_customer_id_idx;
-------------------------------------------------
-- Billing --------------------------------------
CREATE TABLE billing (
    id BIGSERIAL PRIMARY KEY,
    customer_id TEXT UNIQUE,
    subscription_id TEXT,
    subscription_item_id TEXT,
    subscription_status TEXT,
    subscription_current_period_start TIMESTAMPTZ,
    subscription_current_period_end TIMESTAMPTZ,
    plan_id TEXT,
    plan_nickname TEXT,
    card_billing_name TEXT,
    card_brand TEXT,
    card_last4 TEXT,
    card_exp_month SMALLINT,
    card_exp_year SMALLINT,
    created_date TIMESTAMPTZ,
    updated_date TIMESTAMPTZ
);
CREATE INDEX billing_customer_id_idx ON billing (customer_id);
-------------------------------------------------
-- Account --------------------------------------
CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    avatar SMALLINT DEFAULT 0,
    username TEXT UNIQUE NOT NULL,
    password CHAR(60) NOT NULL,
    password_reset_token CHAR(36),
    password_reset_token_expire TIMESTAMPTZ,
    full_name TEXT,
    billing_alert_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    billing_alert_amount SMALLINT NOT NULL DEFAULT 0,
    created_date TIMESTAMPTZ NOT NULL,
    email_subscription_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    billing_id BIGINT NOT NULL REFERENCES billing (id),
    UNIQUE (id, billing_id)
);
CREATE INDEX account_username_idx ON account (username);
CREATE INDEX account_password_reset_token_idx ON account (password_reset_token);
CREATE INDEX account_billing_id_idx ON account (billing_id);
-------------------------------------------------
-- Account Roles --------------------------------
CREATE TABLE account_roles (
    account_id BIGINT NOT NULL REFERENCES account (id),
    name TEXT NOT NULL,
    UNIQUE (account_id, name)
);
CREATE INDEX account_roles_account_id_idx ON account_roles (account_id);
-------------------------------------------------
-- Scope ----------------------------------------
CREATE TABLE scope (
    id BIGSERIAL PRIMARY KEY,
    image BOOLEAN NOT NULL DEFAULT FALSE,
    gif BOOLEAN NOT NULL DEFAULT FALSE,
    text_toxicity BOOLEAN NOT NULL DEFAULT FALSE,
    text_severe_toxicity BOOLEAN NOT NULL DEFAULT FALSE,
    text_identity_attack BOOLEAN NOT NULL DEFAULT FALSE,
    text_insult BOOLEAN NOT NULL DEFAULT FALSE,
    text_profanity BOOLEAN NOT NULL DEFAULT FALSE,
    text_threat BOOLEAN NOT NULL DEFAULT FALSE,
    video BOOLEAN NOT NULL DEFAULT FALSE
);
-------------------------------------------------
-- API Key --------------------------------------
CREATE TABLE api_key (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    prefix TEXT NOT NULL,
    hash TEXT UNIQUE NOT NULL,
    created_date TIMESTAMPTZ NOT NULL,
    last_used_date TIMESTAMPTZ,
    modified_date TIMESTAMPTZ,
    deleted_date TIMESTAMPTZ,
    enabled BOOLEAN NOT NULL,
    account_id BIGINT NOT NULL REFERENCES account (id),
    scope_id BIGINT NOT NULL REFERENCES scope (id),
    UNIQUE (id, scope_id)
);
CREATE INDEX api_key_prefix_idx ON api_key (prefix);
CREATE INDEX api_key_account_id_idx ON api_key (account_id);
CREATE INDEX api_key_scope_id_idx ON api_key (scope_id);
-------------------------------------------------
-- API Key Usage --------------------------------
CREATE TABLE api_key_usage (
    id BIGSERIAL PRIMARY KEY,
    usage_date DATE NOT NULL,
    text_operations BIGINT NOT NULL DEFAULT 0,
    image_operations BIGINT NOT NULL DEFAULT 0,
    api_key_id BIGINT NOT NULL REFERENCES api_key (id),
    UNIQUE (usage_date, api_key_id)
);
CREATE INDEX api_key_usage_usage_date_idx ON api_key_usage (usage_date);
-------------------------------------------------
-- Access Policy --------------------------------
CREATE TABLE access_policy (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    range TEXT NOT NULL,
    api_key_id BIGINT NOT NULL REFERENCES api_key (id),
    UNIQUE (range, api_key_id)
);
CREATE INDEX access_policy_api_key_id_idx ON access_policy (api_key_id);
-------------------------------------------------
-- Billing Log ----------------------------------
CREATE TABLE billing_log (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account (id),
    action TEXT NOT NULL,
    amount TEXT,
    created_date TIMESTAMPTZ NOT NULL
);
CREATE INDEX billing_log_account_id_idx ON billing_log (account_id);
-------------------------------------------------
-- API Key Log ----------------------------------
CREATE TABLE api_key_log (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL REFERENCES api_key (id),
    action TEXT NOT NULL,
    ip TEXT,
    city_name TEXT,
    country_iso_code TEXT,
    subdivision_iso_code TEXT,
    operating_system_family TEXT,
    operating_system_major TEXT,
    operating_system_minor TEXT,
    user_agent_family TEXT,
    user_agent_major TEXT,
    user_agent_minor TEXT,
    created_date TIMESTAMPTZ NOT NULL
);
CREATE INDEX api_key_log_api_key_id_idx ON api_key_log (api_key_id);
-------------------------------------------------
-- Security Log ---------------------------------
CREATE TABLE security_log (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account (id),
    action TEXT NOT NULL,
    ip TEXT,
    city_name TEXT,
    country_iso_code TEXT,
    subdivision_iso_code TEXT,
    operating_system_family TEXT,
    operating_system_major TEXT,
    operating_system_minor TEXT,
    user_agent_family TEXT,
    user_agent_major TEXT,
    user_agent_minor TEXT,
    created_date TIMESTAMPTZ NOT NULL
);
CREATE INDEX security_log_account_id_idx ON security_log (account_id);
-------------------------------------------------
-- Refresh Token --------------------------------
CREATE TABLE refresh_token (
    token UUID PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account (id),
    created_date TIMESTAMPTZ NOT NULL,
    expiration_time TIMESTAMPTZ NOT NULL,
    ip TEXT,
    city_name TEXT,
    country_iso_code TEXT,
    subdivision_iso_code TEXT,
    operating_system_family TEXT,
    operating_system_major TEXT,
    operating_system_minor TEXT,
    user_agent_family TEXT,
    user_agent_major TEXT,
    user_agent_minor TEXT,
    UNIQUE (token, account_id, expiration_time)
);
CREATE INDEX refresh_token_token_idx ON refresh_token (token);