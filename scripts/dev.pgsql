delete from api_key_log;
delete from billing_log;
delete from security_log;
delete from access_policy;
delete from api_key;
delete from scope;
delete from account_roles;
delete from request_metadata;
delete from account;

select * from access_policy;
select * from account;
select * from account_roles;
select * from api_key;
select * from api_key_log;
select * from billing_log;
select * from flyway_schema_history;
select * from scope;
select * from security_log;
select * from request_metadata;