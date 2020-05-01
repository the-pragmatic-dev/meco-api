-------------------------------------------------
-- Test Data Only -------------------------------
-------------------------------------------------
-- Account
INSERT INTO account(id, username, password, password_reset_token, password_reset_token_expire, full_name, billing_alert_enabled, created_date, email_subscription_enabled) values (1, 'admin@email.com', '$2a$12$kx9DDIZWgPlg8A7M1z/GFeHQy0fFkn3it18XTNNpNnCO6MjGs/hXm', null, null, 'Stephen Cathcart', false, '2020-02-25T10:30:44.232Z', true) ON CONFLICT DO NOTHING;
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
INSERT INTO billing_log values (1, 1, 'billing.paid', '-£50.00', '2020-02-26T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO billing_log values (2, 1, 'billing.invoice', '£0.00', '2020-02-25T15:50:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO billing_log values (3, 1, 'subscription.created', '£0.00', '2020-02-24T15:55:19.111Z') ON CONFLICT DO NOTHING;
ALTER SEQUENCE billing_log_id_seq RESTART WITH 4;
-- API Key Log
INSERT INTO api_key_log values (1, 1, 'image.predict', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-24T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO api_key_log values (2, 1, 'gif.predict', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-25T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO api_key_log values (3, 1, 'text.predict', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-26T15:40:19.111Z') ON CONFLICT DO NOTHING;
ALTER SEQUENCE api_key_log_id_seq RESTART WITH 4;
-- Security Log
INSERT INTO security_log values (1, 1, 'account.created', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-24T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO security_log values (2, 1, 'user.two_factor_successful_login', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-25T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO security_log values (3, 1, 'user.login', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-26T15:40:19.111Z') ON CONFLICT DO NOTHING;
ALTER SEQUENCE security_log_id_seq RESTART WITH 4;