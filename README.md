[![Build Status](https://travis-ci.com/the-pragmatic-dev/meco-api.svg?branch=master)](https://travis-ci.com/the-pragmatic-dev/meco-api)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=alert_status)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=bugs)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=ncloc)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=duplicated_lines_density)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=coverage)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=code_smells)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=security_rating)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=the-pragmatic-dev_meco-api&metric=sqale_index)](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api)
[![Discord](https://badgen.net/badge/icon/discord?icon=discord&label)](https://discord.gg/MaYf3e)
[![License](https://badgen.net/badge/license/Apache-2.0/blue)](LICENSE)

# MECO API

JSON API for account, billing & key management services. Aimed to moderate explicit content online :underage:.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Project structure

The project folder structure is shown below.

* **dev-scripts**: useful exported PostgreSQL scripts for development.
* **postman**: exported v2.1 [Postman](https://www.getpostman.com/) JSON collection file for testing.
* **src**: contains project source code.

### Prerequisites

Make sure you have installed all of the following prerequisites on your development machine:

* Download & install [Git](https://git-scm.com/). OSX and Linux machines typically have this already installed.
* Install the latest version of [Java](https://openjdk.java.net/).
* Rather than installing [Maven](https://maven.apache.org/) manually you may use the Maven Wrapper `mvnw` or `mvnw.cmd`.
* An IDE plugin for [Project Lombok](https://projectlombok.org/) such as [this](https://marketplace.visualstudio.com/items?itemName=GabrielBB.vscode-lombok).
* You may need to set your `JAVA_HOME` environment variable.
* A [PostgreSQL](https://www.postgresql.org/) database for storing account, billing and key data.
* A [Stripe](https://stripe.com/gb) account with access to your [secret key](https://stripe.com/docs/keys) for processing payments.
* A [Mailgun](https://www.mailgun.com/) account with access to your [secret key](https://help.mailgun.com/hc/en-us/articles/203380100-Where-Can-I-Find-My-API-Key-and-SMTP-Credentials-) for sending emails.
* Optional: An API testing tool such as [Postman](https://www.postman.com/) for exploratory API testing.

The recommended way to get the MECO API is to use Git to directly clone the repository:

```bash
# clone repository
git clone https://github.com/the-pragmatic-dev/meco-api.git
cd meco-api/
```

Default properties found in `src/main/resources/application.yml` will need to be updated. You must ignore local updates to `application.yml` to protect your secret keys. Mark the file as so: `git update-index --skip-worktree src/main/resources/application.yml`. Either set environment variables using the `export` command or pass them to the application at runtime:

```bash
# set environment properties and run service
export DATASOURCE_URL=jdbc:postgresql://{host}:{port}/{database}
export DATASOURCE_USERNAME=username
export DATASOURCE_PASSWORD=password
...
./mvnw spring-boot:run

# or set properties at runtime
./mvnw spring-boot:run -Dspring-boot.run.arguments= \
  --logging.file.path={path}, \
  --server.port={port}, \
  --spring.datasource.url={url}, \
  --spring.datasource.username={username}, \
  --spring.datasource.password={password}, \
  --spring.jpa.show-sql=false, \
  --spring.flyway.clean-on-validation-error=false, \
  --spring.flyway.locations=classpath:/db/migration,classpath:/db/data/prod, \
  --security.jwt.token.secret-key={key}, \
  --security.jwt.token.expire-length=300000, \
  --stripe.secret-key={key}, \
  --mailgun.secret-key={key}
```

Spring JPA is set to only validate the DDL - database configuration management is done through [Flyway](https://flywaydb.org/). Flyway will configure the database schema on startup by running all migration files under `src/main/resources/db/migration`. By default, a dummy admin account is created with the following credentials: username: `admin@email.com`, password: `password`.

### Installing

From the cloned workspace compile the project which will also run all unit tests:

```bash
# compile and run
./mvnw install
```

the output from a successful build is below.

```bash
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  2.001 s
[INFO] Finished at: 2020-04-01T20:50:20+01:00
[INFO] ------------------------------------------------------------------------
```

## Running the tests

Maven profiles exist for running either the unit tests or integration tests, `unit` and `integration` respectively. The `unit` profile is active by default.

### Unit tests

You can run unit tests by running the following command at the command prompt:

```bash
./mvnw clean test
```

### Integration tests

You can run integration tests by running the following command at the command prompt:

```bash
./mvnw clean verify -P integration
```

### Sonar report

Sonar properties are defined in `pom.xml`. Test reports are pushed to [SonarCloud](https://sonarcloud.io/dashboard?id=the-pragmatic-dev_meco-api) using the following command on our [Travis CI](https://travis-ci.com/github/the-pragmatic-dev/meco-api) build server:

```bash
./mvnw sonar:sonar
```

### Coding style guide

The Maven [Checkstyle](https://github.com/checkstyle/checkstyle) plugin will check for violations before compiling. Code that does not adhere to the [Google Java Style Guide](https://checkstyle.sourceforge.io/styleguides/google-java-style-20180523/javaguide.html#s1-introduction) as defined in `checkstyle.google.xml` will cause the build to fail.

## Deployment

Terraform is used for provisioning infrastructure through code. We use it to create a our DigitalOcean environment.

### Prerequisites

1. Install [Terraform CLI](https://www.terraform.io/downloads.html) (version >= 0.12) and add it to your `PATH`.
2. Generate SSH keys and add to Digital Ocean. On the menu `Account > API > Add SSH key` . After you’ve added, it will show you a fingerprint that you will need to run the terraform command.
3. Generate a DigitalOcean API token. Menu `Manage > API > Generate New Token`.

### Terraform

Terraform files are located under the `terraform` directory. In order to run them, you need to pass the variables as params (or Terraform will ask one by one). You can configure env vars as follows:

```bash
export DO_TOKEN=xxx
export SSH_FINGERPRINT=xxx
```

Also we need to call `init` to download the provider plugins:

```bash
terraform init
```

And then finally apply it:

```bash
terraform apply \
  -var "do_token=$DO_TOKEN" \
  -var "pub_key=$HOME/.ssh/id_rsa.pub" \
  -var "pvt_key=$HOME/.ssh/id_rsa" \
  -var "ssh_fingerprint=$SSH_FINGERPRINT"
```

It first shows the plan that is about to be executed. If the plan looks ok, type `yes` (or use `--auto-aprove`). The environment will then be built.

For development purposes you can destroy the newly created environment by running `destroy`:

```bash
terraform destroy \
  -var "do_token=$DO_TOKEN" \
  -var "pub_key=$HOME/.ssh/id_rsa.pub" \
  -var "pvt_key=$HOME/.ssh/id_rsa" \
  -var "ssh_fingerprint=$SSH_FINGERPRINT"
```

### API documentation

Once the service is running, the API documentation will be available [here](http://localhost:8080/swagger-ui/index.html?url=/v3/api-docs&validatorUrl=).

### Key generation

Sensitive fields in domain models are set to be ignored on JSON serialisation by default, such as account password hashes and the actual API key. To allow the key to be viewed only once
by the user on creation, [Programmatic JSON Views](https://github.com/monitorjbl/json-view) are used to include selected fields on POST requests.

### Authorization

JWT is used for authorization. JWTs are signed using a secret key. Once a user is logged in, each subsequent request will require the JWT, allowing the user to access routes, services, and resources that are permitted with that token. Below is an example of authorizing with the API and using the JWT in a subsequent request:

```curl
# signin to service
curl -X POST "http://localhost:8080/accounts/signin" -H "accept: */*" -H "Content-Type: application/json" -d "{\"username\":\"admin@email.com\",\"password\":\"password\"}"

# send JWT (token) with new request which lists all API keys
curl -X GET "http://localhost:8080/api-keys" -H "accept: application/json" -H "Authorization: Bearer {token}"

```

JWT claims contain both the account username and given roles.

## Monitoring

Actuator endpoints are enabled by default to monitor and interact with the running service.

* **endpoints**: [http://{host}:{port}/actuator/](http://localhost:8080/actuator).
* **health**: [http://{host}:{port}/actuator/health](http://localhost:8080/actuator/health).
* **info**: [http://{host}:{port}/actuator/info](http://localhost:8080/actuator/info).
* **flyway**: [http://{host}:{port}/actuator/flyway](http://localhost:8080/actuator/flyway).
* **metrics**: [http://{host}:{port}/actuator/metrics](http://localhost:8080/actuator/metrics).
* **metrics-name**: [http://{host}:{port}/actuator/metrics/{requiredMetricName}](http://localhost:8080/actuator/metrics/system.cpu.count).

## Built With

* [Spring Boot](https://spring.io/projects/spring-boot) - Java-based framework.
* [Project Lombok](https://projectlombok.org/) - Java library to minimize boilerplate.
* [Stripe Java](https://github.com/stripe/stripe-java) - Java library for the Stripe API.
* [Mailgun Java](https://github.com/sargue/mailgun) - Java library for the Mailgun API.
* [Programmatic JSON Views](https://github.com/monitorjbl/json-view) - Java library to include selected JSON fields.
* [Checkstyle](https://github.com/checkstyle/checkstyle) - Java tool for checking source code adheres to Google code standard.
* [JWT](https://jwt.io/) - JSON-based access tokens that assert a number of claims.
* [Maven](https://maven.apache.org/) - Java dependency management.
* [Springdoc OpenApi](https://github.com/springdoc/springdoc-openapi) - Provides API documentation.
* [PostgreSQL](https://www.postgresql.org/) - Relational database.
* [Flyway](https://flywaydb.org/) - Database configuration management.

## Documentation

* [Account service](src/main/java/uk/thepragmaticdev/account/README.md) - Management of a users account.
* [Billing service](src/main/java/uk/thepragmaticdev/billing/README.md) - Management of account subscriptions.
* [Email service](src/main/java/uk/thepragmaticdev/email/README.md) - Management of account email triggers.
* [Key management service](src/main/java/uk/thepragmaticdev/kms/README.md) - Management of creating, updating and deleting API keys.

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/the-pragmatic-dev/meco-api/tags).

## Authors

* **Stephen Cathcart** - *Initial work* - [The Pragmatic Dev Ltd](https://thepragmaticdev.uk/)

See also the list of [contributors](https://github.com/the-pragmatic-dev/meco-api/graphs/contributors) who participated in this project.

## License

MECO API is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full license text.

## Acknowledgments

* **Hugo Firth** - *Tech consultant* - [GitHub](https://github.com/hugofirth)

