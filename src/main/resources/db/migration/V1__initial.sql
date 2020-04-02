-------------------------------------------------
-- Account --------------------------------------
DROP TABLE IF EXISTS account;
CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    full_name TEXT NOT NULL,
    billing_alert_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_date TIMESTAMPTZ NOT NULL,
    email_subscription_enabled BOOLEAN NOT NULL DEFAULT FALSE
);
DROP INDEX IF EXISTS account_username_idx;
CREATE INDEX account_username_idx ON account (username);
-------------------------------------------------
-- Account Roles --------------------------------
DROP TABLE IF EXISTS account_roles;
CREATE TABLE account_roles (
    account_id BIGINT NOT NULL REFERENCES account (id),
    name TEXT NOT NULL
);
DROP INDEX IF EXISTS account_roles_account_id_idx;
CREATE INDEX account_roles_account_id_idx ON account_roles (account_id);
-------------------------------------------------
-- Scope ----------------------------------------
DROP TABLE IF EXISTS scope;
CREATE TABLE scope (
    id BIGSERIAL PRIMARY KEY,
    image BOOLEAN NOT NULL DEFAULT FALSE,
    gif BOOLEAN NOT NULL DEFAULT FALSE,
    text BOOLEAN NOT NULL DEFAULT FALSE,
    video BOOLEAN NOT NULL DEFAULT FALSE
);
-------------------------------------------------
-- API Key --------------------------------------
DROP TABLE IF EXISTS api_key;
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
    UNIQUE (ID, scope_id)
);
DROP INDEX IF EXISTS api_key_hash_idx;
DROP INDEX IF EXISTS api_key_account_id_idx;
DROP INDEX IF EXISTS api_key_scope_id_idx;
CREATE INDEX api_key_hash_idx ON api_key (hash);
CREATE INDEX api_key_account_id_idx ON api_key (account_id);
CREATE INDEX api_key_scope_id_idx ON api_key (scope_id);
-------------------------------------------------
-- Access Policy --------------------------------
DROP TABLE IF EXISTS access_policy;
CREATE TABLE access_policy (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    range TEXT NOT NULL,
    api_key_id BIGINT REFERENCES api_key (id),
    UNIQUE (range, api_key_id)
);
DROP INDEX IF EXISTS access_policy_api_key_id_idx;
CREATE INDEX access_policy_api_key_id_idx ON access_policy (api_key_id);
-------------------------------------------------
-- Billing Log ----------------------------------
DROP TABLE IF EXISTS billing_log;
CREATE TABLE billing_log (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account (id),
    action TEXT NOT NULL,
    amount TEXT NOT NULL,
    instant TIMESTAMPTZ NOT NULL
);
DROP INDEX IF EXISTS billing_log_account_id_idx;
CREATE INDEX billing_log_account_id_idx ON billing_log (account_id);
-------------------------------------------------
-- API Key Log ----------------------------------
DROP TABLE IF EXISTS api_key_log;
CREATE TABLE api_key_log (
    id BIGSERIAL PRIMARY KEY,
    api_key_id BIGINT NOT NULL REFERENCES api_key (id),
    action TEXT NOT NULL,
    address TEXT NOT NULL,
    instant TIMESTAMPTZ NOT NULL
);
DROP INDEX IF EXISTS api_key_log_api_key_id_idx;
CREATE INDEX api_key_log_api_key_id_idx ON api_key_log (api_key_id);
-------------------------------------------------
-- Security Log ---------------------------------
DROP TABLE IF EXISTS security_log;
CREATE TABLE security_log (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES account (id),
    action TEXT NOT NULL,
    address TEXT NOT NULL,
    instant TIMESTAMPTZ NOT NULL
);
DROP INDEX IF EXISTS security_log_account_id_idx;
CREATE INDEX security_log_account_id_idx ON security_log (account_id);

-------------------------------------------------
-- Test Data Only -------------------------------
-------------------------------------------------
-- Account
INSERT INTO account(id, username, password, full_name, created_date, email_subscription_enabled) values (1, 'admin@email.com', '$2a$12$kx9DDIZWgPlg8A7M1z/GFeHQy0fFkn3it18XTNNpNnCO6MjGs/hXm', 'Stephen Cathcart', '2020-02-25T10:30:44.232Z', true) ON CONFLICT DO NOTHING;
ALTER SEQUENCE account_id_seq RESTART WITH 2;
-- Account Roles
INSERT INTO account_roles (account_id, name) values (1, 'ROLE_ADMIN') ON CONFLICT DO NOTHING;
-- Scope
INSERT INTO scope(id, image, gif, text, video) values (1, false, true, true, false) ON CONFLICT DO NOTHING;
INSERT INTO scope(id, image, gif, text, video) values (2, true, false, false, true) ON CONFLICT DO NOTHING;
ALTER SEQUENCE scope_id_seq RESTART WITH 3;
-- API Key
INSERT INTO api_key(id, name, prefix, hash, created_date, last_used_date, modified_date, enabled, account_id, scope_id) values 
(1, 'Good Coffee Shop', 'zaCELgL', '$2a$12$kx9DDIZWgPlg8A7M1z/GFeHQy0fFkn3it18XTNNpNnCO6MjGs/hXm', '2020-02-25T13:38:58.232Z', null, '2020-02-25T13:40:19.111Z', true, 1, 1) ON CONFLICT DO NOTHING;
INSERT INTO api_key(id, name, prefix, hash, created_date, last_used_date, modified_date, enabled, account_id, scope_id) values 
(2, 'Bobs Pastry Shop', '7Cx9VYK', '$2a$12$Jl0WgfT1aGoTEDVAYeJJveOZMcR/m9bz2A5QQsbVH4/AX5fVv9P3W', '2020-02-25T15:06:41.718Z', null, null, true, 1, 2) ON CONFLICT DO NOTHING;
ALTER SEQUENCE api_key_id_seq RESTART WITH 3;
-- Access Policy
INSERT INTO access_policy(id, name, range, api_key_id) values (1, 'newcastle', '5.65.196.0/16', 1) ON CONFLICT DO NOTHING;
INSERT INTO access_policy(id, name, range, api_key_id) values (2, 'quedgeley', '17.22.136.0/32', 1) ON CONFLICT DO NOTHING;
ALTER SEQUENCE access_policy_id_seq RESTART WITH 3;
-- Billing Log
INSERT INTO billing_log (id, account_id, action, amount, instant) values (1, 1, 'billing.paid', '-£50.00', '2020-02-25T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO billing_log (id, account_id, action, amount, instant) values (2, 1, 'billing.invoice', '£0.00', '2020-02-25T15:50:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO billing_log (id, account_id, action, amount, instant) values (3, 1, 'subscription.created', '£0.00', '2020-02-25T15:55:19.111Z') ON CONFLICT DO NOTHING;
ALTER SEQUENCE billing_log_id_seq RESTART WITH 4;
-- API Key Log
INSERT INTO api_key_log(id, api_key_id, action, address, instant) values (1, 1, 'image.predict', '5.65.196.222', '2020-02-25T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO api_key_log(id, api_key_id, action, address, instant) values (2, 1, 'gif.predict', '5.65.196.222', '2020-02-25T16:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO api_key_log(id, api_key_id, action, address, instant) values (3, 1, 'text.predict', '5.65.196.222', '2020-02-25T17:40:19.111Z') ON CONFLICT DO NOTHING;
ALTER SEQUENCE api_key_log_id_seq RESTART WITH 4;
-- Security Log
INSERT INTO security_log (id, account_id, action, address, instant) values (1, 1, 'account.created', '5.65.196.222', '2020-02-24T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO security_log (id, account_id, action, address, instant) values (2, 1, 'user.two_factor_successful_login', '5.65.196.222', '2020-02-25T15:50:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO security_log (id, account_id, action, address, instant) values (3, 1, 'user.login', '5.65.196.222', '2020-02-25T15:51:19.111Z') ON CONFLICT DO NOTHING;
ALTER SEQUENCE security_log_id_seq RESTART WITH 4;