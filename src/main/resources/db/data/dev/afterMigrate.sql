-------------------------------------------------
-- Test Data Only -------------------------------
-------------------------------------------------
-- Billing
INSERT INTO billing values(1, null, null, null, null, null, null, null, null, null, null, null, null, null, '2020-02-25T10:30:44.232Z', null) ON CONFLICT DO NOTHING;
SELECT setval('billing_id_seq', max(id)) FROM billing;
-- Account
INSERT INTO account values (1, 0, 'admin@email.com', '$2a$12$kx9DDIZWgPlg8A7M1z/GFeHQy0fFkn3it18XTNNpNnCO6MjGs/hXm', null, null, 'Stephen Cathcart', false, 0, '2020-02-25T10:30:44.232Z', true, 1) ON CONFLICT DO NOTHING;
SELECT setval('account_id_seq', max(id)) FROM account;
-- Account Roles
INSERT INTO account_roles (account_id, name) values (1, 'ROLE_ADMIN') ON CONFLICT DO NOTHING;
-- Scope
INSERT INTO scope values (1, false, true, true, true, true, true, true, true, false) ON CONFLICT DO NOTHING;
INSERT INTO scope values (2, true, false, false, false, false, false, false, false, true) ON CONFLICT DO NOTHING;
SELECT setval('scope_id_seq', max(id)) FROM scope;
-- API Key
INSERT INTO api_key(id, name, prefix, hash, created_date, last_used_date, modified_date, deleted_date, enabled, account_id, scope_id) values 
(1, 'Good Coffee Shop', 'rAosN1E', '$2a$12$vEFf.RpLuHuHDL3mEbORMOyvm1/Jgbz04wOJ1qOE4IyUUA2H2Ps4O', '2020-02-25T13:38:58.232Z', null, '2020-02-25T13:40:19.111Z', null, true, 1, 1) ON CONFLICT DO NOTHING;
INSERT INTO api_key(id, name, prefix, hash, created_date, last_used_date, modified_date, deleted_date, enabled, account_id, scope_id) values 
(2, 'Bobs Pastry Shop', '7Cx9VYK', '$2a$12$Jl0WgfT1aGoTEDVAYeJJveOZMcR/m9bz2A5QQsbVH4/AX5fVv9P3W', '2020-02-25T15:06:41.718Z', null, null, null, true, 1, 2) ON CONFLICT DO NOTHING;
SELECT setval('api_key_id_seq', max(id)) FROM api_key;
-- Access Policy
INSERT INTO access_policy(id, name, range, api_key_id) values (1, 'newcastle', '5.65.196.0/16', 1) ON CONFLICT DO NOTHING;
INSERT INTO access_policy(id, name, range, api_key_id) values (2, 'quedgeley', '17.22.136.0/32', 1) ON CONFLICT DO NOTHING;
SELECT setval('access_policy_id_seq', max(id)) FROM access_policy;
-- Billing Log
INSERT INTO billing_log values (1, 1, 'billing.paid', '-£50.00', '2020-02-26T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO billing_log values (2, 1, 'billing.invoice', '£0.00', '2020-02-25T15:50:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO billing_log values (3, 1, 'subscription.created', '£0.00', '2020-02-24T15:55:19.111Z') ON CONFLICT DO NOTHING;
SELECT setval('billing_log_id_seq', max(id)) FROM billing_log;
-- API Key Log
INSERT INTO api_key_log values (1, 1, 'image.predict', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-24T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO api_key_log values (2, 1, 'gif.predict', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-25T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO api_key_log values (3, 1, 'text.predict', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-26T15:40:19.111Z') ON CONFLICT DO NOTHING;
SELECT setval('api_key_log_id_seq', max(id)) FROM api_key_log;
-- Security Log
INSERT INTO security_log values (1, 1, 'account.created', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-24T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO security_log values (2, 1, 'account.two_factor_successful_login', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-25T15:40:19.111Z') ON CONFLICT DO NOTHING;
INSERT INTO security_log values (3, 1, 'account.signin', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0', '2020-02-26T15:40:19.111Z') ON CONFLICT DO NOTHING;
SELECT setval('security_log_id_seq', max(id)) FROM security_log;
-- Refresh Token
INSERT INTO refresh_token values ('08fa878c-1d28-40d3-a3ef-5a52c649840c', 1, '2020-01-01T00:00:00.000Z', '3000-01-01T00:00:00.000Z', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0') ON CONFLICT DO NOTHING;
INSERT INTO refresh_token values ('ee42cae2-a012-11ea-bb37-0242ac130002', 1, '2020-01-01T00:00:00.000Z', '3000-01-01T00:00:00.000Z', '196.245.163.202', 'London', 'GB', 'ENG', 'Linux', '0', '0', 'Chrome', '78', '0') ON CONFLICT DO NOTHING;
INSERT INTO refresh_token values ('624aa34c-a00e-11ea-bb37-0242ac130002', 1, '2020-01-01T00:00:00.000Z', '2020-01-01T00:00:00.000Z', '196.245.163.202', 'London', 'GB', 'ENG', 'Mac OS X', '10', '14', 'Chrome', '71', '0') ON CONFLICT DO NOTHING;