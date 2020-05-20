package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.stripe.exception.StripeException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Scanner;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.log.security.SecurityLog;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class AccountEndpointIT extends IntegrationData {
  // @formatter:off

  @Autowired
  private EmailService emailService;

  @Autowired
  private AccountService accountService;

  @Autowired
  private BillingService billingService;

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  // @endpoint:signup

  @Test
  void shouldCreateAccount() throws StripeException {
    var request = accountSignupRequest();
    request.setUsername("account@integration.test");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(ACCOUNTS_ENDPOINT + "signup")
    .then()
        .body("token", not(emptyString()))
        .statusCode(201);
    // clean up stripe customer created on account creation
    var account = accountService.findAuthenticatedAccount("account@integration.test");
    billingService.deleteCustomer(account.getUsername());
  }

  // @endpoint:me

  @Test
  void shouldReturnAuthenticatedAccount() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("id", is(nullValue()))
        .body("stripeCustomerId", is(nullValue()))
        .body("stripeSubscriptionId", is(nullValue()))
        .body("stripeSubscriptionItemId", is(nullValue()))
        .body("username", is("admin@email.com"))
        .body("password", is(nullValue()))
        .body("passwordResetToken", is(nullValue()))
        .body("passwordResetTokenExpire", is(nullValue()))
        .body("fullName", is("Stephen Cathcart"))
        .body("emailSubscriptionEnabled", is(true))
        .body("billingAlertEnabled", is(false))
        .body("createdDate", is("2020-02-25T10:30:44.232Z"))
        .body("roles", is(nullValue()))
        .body("apiKeys", is(nullValue()))
        .statusCode(200);
  }

  // @endpoint:me/forgot

  @Test
  void shouldReturnOkWhenForgottenPassword() {
    given()
      .headers(headers())
      .queryParam("username", "admin@email.com")
    .when()
      .post(ACCOUNTS_ENDPOINT + "me/forgot")
    .then()
        .statusCode(200);
  }

  // @endpoint:me/reset

  @Test
  void shouldReturnOkWhenResetPassword() {
    accountService.forgot("admin@email.com");
    var captor = ArgumentCaptor.forClass(Account.class);
    verify(emailService, atLeastOnce()).sendForgottenPassword(captor.capture());
    var actual = captor.getValue();
    var diffMinutes = ChronoUnit.MINUTES.between(OffsetDateTime.now(), actual.getPasswordResetTokenExpire());
    assertThat(actual.getPasswordResetToken(), is(not(emptyString())));
    assertThat(diffMinutes, is(1439L)); // 23 hours and 59 minutes

    var request = accountResetRequest();
    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
      .queryParam("token", actual.getPasswordResetToken())
    .when()
      .post(ACCOUNTS_ENDPOINT + "me/reset")
    .then()
        .statusCode(200);
  }

  // @endpoint:update

  @Test
  void shouldUpdateOnlyMutableAccountFields() {
    var request = accountUpdateRequest();

    given()
      .headers(headers())
      .contentType(JSON)
      .header(HttpHeaders.AUTHORIZATION, signin())
      .body(request)
    .when()
      .put(ACCOUNTS_ENDPOINT + "me")
    .then()
        .body("stripeCustomerId", is(nullValue()))
        .body("stripeSubscriptionId", is(nullValue()))
        .body("stripeSubscriptionItemId", is(nullValue()))
        .body("username", is("admin@email.com"))
        .body("password", is(nullValue()))
        .body("passwordResetToken", is(nullValue()))
        .body("passwordResetTokenExpire", is(nullValue()))
        .body("fullName", is(request.getFullName()))
        .body("emailSubscriptionEnabled", is(request.getEmailSubscriptionEnabled()))
        .body("billingAlertEnabled", is(request.getBillingAlertEnabled()))
        .body("createdDate", is("2020-02-25T10:30:44.232Z"))
        .statusCode(200);
  }

  // @endpoint:billing-logs

  @Test
  void shouldReturnLatestBillingLogs() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs")
    .then()
        .body("numberOfElements", is(3))
        .body("content", hasSize(3))
        .root("content[0]")
          .body("action", is("billing.paid"))
          .body("amount", is("-£50.00"))
          .body("createdDate", is("2020-02-26T15:40:19.111Z"))
        .root("content[1]")
          .body("action", is("billing.invoice"))
          .body("amount", is("£0.00"))
          .body("createdDate", is("2020-02-25T15:50:19.111Z"))
        .root("content[2]")
          .body("action", is("subscription.created"))
          .body("amount", is("£0.00"))
          .body("createdDate", is("2020-02-24T15:55:19.111Z"))
        .statusCode(200);
  }

  // @endpoint:billing-logs-download

  @Test
  void shouldDownloadBillingLogs() throws IOException {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/billing/logs/download")
    .then()
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, is(HttpHeaders.CONTENT_DISPOSITION))
        .header(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename="))
        .body(is(csv("data/billing.log.csv")))
        .statusCode(200);
  }

  // @endpoint:security-logs

  @Test
  void shouldReturnLatestSecurityLogs() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(ACCOUNTS_ENDPOINT + "me/security/logs")
    .then()
        .body("numberOfElements", is(4))
        .body("content", hasSize(4))
        .root("content[0]")
          .body("action", is("account.signin"))
          .body("createdDate", is(withinLast(5, ChronoUnit.SECONDS)))
          .spec(validRequestMetadataSpec(0))
        .root("content[1]")
          .body("action", is("account.signin"))
          .body("createdDate", is("2020-02-26T15:40:19.111Z"))
          .spec(validRequestMetadataSpec(0))
        .root("content[2]")
          .body("action", is("account.two_factor_successful_login"))
          .body("createdDate", is("2020-02-25T15:40:19.111Z"))
          .spec(validRequestMetadataSpec(1))
        .root("content[3]")
          .body("action", is("account.created"))
          .body("createdDate", is("2020-02-24T15:40:19.111Z"))
          .spec(validRequestMetadataSpec(2))
        .statusCode(200);
  }

  // @endpoint:security-logs-download

  @Test
  void shouldDownloadSecurityLogs() throws IOException {
    var csv = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin())
        .when()
          .get(ACCOUNTS_ENDPOINT + "me/security/logs/download")
        .then()
          .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, is(HttpHeaders.CONTENT_DISPOSITION))
          .header(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename="))
          .statusCode(200)
        .extract().body().asString();
    assertCsvMatch(csv, expectedSecurityLogs());
  }

  // @formatter:on

  // @helpers:security-log-assertions

  private List<SecurityLog> expectedSecurityLogs() {
    return List.of(//
        expectedSecurityLog("account.signin", "1900-01-01T00:00:00Z"), //
        expectedSecurityLog("account.signin", "2020-02-26T15:40:19.111Z"), //
        expectedSecurityLog("account.two_factor_successful_login", "2020-02-25T15:40:19.111Z"), //
        expectedSecurityLog("account.created", "2020-02-24T15:40:19.111Z")//
    );
  }

  private SecurityLog expectedSecurityLog(String action, String offsetDateTime) {
    return new SecurityLog(null, null, action, requestMetadata(), OffsetDateTime.parse(offsetDateTime));
  }

  private void assertCsvMatch(String csv, List<SecurityLog> expectedSecurityLogs) {
    try (var scanner = new Scanner(csv)) {
      assertHeaders(scanner.nextLine());
      for (var log : expectedSecurityLogs) {
        var fields = scanner.nextLine().split(",");
        assertSecurityLog(fields, log);
      }
      assertThat(scanner.hasNextLine(), is(false));
    }
  }

  private void assertHeaders(String headers) {
    assertThat(headers,
        is("ACTION,CITYNAME,COUNTRYISOCODE,CREATEDDATE,IP,"
            + "OPERATINGSYSTEMFAMILY,OPERATINGSYSTEMMAJOR,OPERATINGSYSTEMMINOR,"
            + "SUBDIVISIONISOCODE,USERAGENTFAMILY,USERAGENTMAJOR,USERAGENTMINOR"));
  }

  private void assertSecurityLog(String[] fields, SecurityLog log) {
    assertThat(fields[0], is(log.getAction()));
    assertThat(fields[1], is(log.getRequestMetadata().getGeoMetadata().getCityName()));
    assertThat(fields[2], is(log.getRequestMetadata().getGeoMetadata().getCountryIsoCode()));
    // Date should either match expected value or be a date within last five seconds
    // of now.
    assertThat(fields[3], anyOf(is(log.getCreatedDate().toString()), is(withinLast(5, ChronoUnit.SECONDS))));
    assertThat(fields[4], is(log.getRequestMetadata().getIp()));
    assertThat(fields[5], is(log.getRequestMetadata().getDeviceMetadata().getOperatingSystemFamily()));
    assertThat(fields[6], is(log.getRequestMetadata().getDeviceMetadata().getOperatingSystemMajor()));
    assertThat(fields[7], is(log.getRequestMetadata().getDeviceMetadata().getOperatingSystemMinor()));
    assertThat(fields[8], is(log.getRequestMetadata().getGeoMetadata().getSubdivisionIsoCode()));
    assertThat(fields[9], is(log.getRequestMetadata().getDeviceMetadata().getUserAgentFamily()));
    assertThat(fields[10], is(log.getRequestMetadata().getDeviceMetadata().getUserAgentMajor()));
    assertThat(fields[11], is(log.getRequestMetadata().getDeviceMetadata().getUserAgentMinor()));
  }
}