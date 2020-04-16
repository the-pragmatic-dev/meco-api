-------------------------------------------------
-- Drop Tables ----------------------------------
DROP TABLE IF EXISTS api_key_log;
DROP TABLE IF EXISTS security_log;
DROP TABLE IF EXISTS billing_log;
DROP TABLE IF EXISTS access_policy;
DROP TABLE IF EXISTS account_roles;
DROP TABLE IF EXISTS api_key;
DROP TABLE IF EXISTS scope;
DROP TABLE IF EXISTS account;
-------------------------------------------------
-- Drop Indexes ---------------------------------
DROP INDEX IF EXISTS api_key_log_api_key_id_idx;
DROP INDEX IF EXISTS security_log_account_id_idx;
DROP INDEX IF EXISTS billing_log_account_id_idx;
DROP INDEX IF EXISTS access_policy_api_key_id_idx;
DROP INDEX IF EXISTS account_roles_account_id_idx;
DROP INDEX IF EXISTS api_key_hash_idx;
DROP INDEX IF EXISTS api_key_account_id_idx;
DROP INDEX IF EXISTS api_key_scope_id_idx;
DROP INDEX IF EXISTS account_username_idx;
DROP INDEX IF EXISTS account_password_reset_token_idx;
-------------------------------------------------
-- Account --------------------------------------
CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password CHAR(60) NOT NULL,
    password_reset_token CHAR(36),
    full_name TEXT,
    billing_alert_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMPTZ NOT NULL,
    email_subscription_enabled BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX account_username_idx ON account (username);
CREATE INDEX account_password_reset_token_idx ON account (password_reset_token);
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
    text BOOLEAN NOT NULL DEFAULT FALSE,
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
    enabled BOOLEAN NOT NULL,
    account_id BIGINT NOT NULL REFERENCES account (id),
    scope_id BIGINT REFERENCES scope (id),
    UNIQUE (id, scope_id)
);
CREATE INDEX api_key_hash_idx ON api_key (hash);
CREATE INDEX api_key_account_id_idx ON api_key (account_id);
CREATE INDEX api_key_scope_id_idx ON api_key (scope_id);
-------------------------------------------------
-- Access Policy --------------------------------
CREATE TABLE access_policy (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    range TEXT NOT NULL,
    api_key_id BIGINT REFERENCES api_key (id),
    UNIQUE (range, api_key_id)
);
CREATE INDEX access_policy_api_key_id_idx ON access_policy (api_key_id);
-------------------------------------------------
-- Billing Log ----------------------------------
CREATE TABLE billing_log (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account (id),
    action TEXT NOT NULL,
    amount TEXT NOT NULL,
    instant TIMESTAMPTZ NOT NULL
);
CREATE INDEX billing_log_account_id_idx ON billing_log (account_id);
-------------------------------------------------
-- API Key Log ----------------------------------
CREATE TABLE api_key_log (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL REFERENCES api_key (id),
    action TEXT NOT NULL,
    address TEXT NOT NULL,
    instant TIMESTAMPTZ NOT NULL
);
CREATE INDEX api_key_log_api_key_id_idx ON api_key_log (api_key_id);
-------------------------------------------------
-- Security Log ---------------------------------
CREATE TABLE security_log (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account (id),
    action TEXT NOT NULL,
    address TEXT NOT NULL,
    instant TIMESTAMPTZ NOT NULL
);
CREATE INDEX security_log_account_id_idx ON security_log (account_id);