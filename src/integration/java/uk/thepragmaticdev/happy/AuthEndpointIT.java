package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.stripe.exception.StripeException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.auth.AuthService;
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.email.EmailService;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class AuthEndpointIT extends IntegrationData {
  // @formatter:off

  @Autowired
  private EmailService emailService;

  @Autowired
  private AccountService accountService;

  @Autowired
  private AuthService authService;

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
    var request = authSignupRequest();
    request.setUsername("auth@integration.test");

    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "signup")
    .then()
        .body("accessToken", is(not(emptyString())))
        .body("refreshToken", is(not(emptyString())))
        .statusCode(201);
    // clean up stripe customer created on account creation
    var account = accountService.findAuthenticatedAccount("auth@integration.test");
    billingService.deleteCustomer(account.getUsername());
  }

  // @endpoint:forgot

  @Test
  void shouldReturnOkWhenForgottenPassword() {
    given()
      .headers(headers())
      .queryParam("username", "admin@email.com")
    .when()
      .post(AUTH_ENDPOINT + "forgot")
    .then()
        .statusCode(200);
  }
  
  // @endpoint:reset

  @Test
  void shouldReturnOkWhenResetPassword() {
    authService.forgot("admin@email.com");
    var captor = ArgumentCaptor.forClass(Account.class);
    verify(emailService, atLeastOnce()).sendForgottenPassword(captor.capture());
    var actual = captor.getValue();
    var diffMinutes = ChronoUnit.MINUTES.between(OffsetDateTime.now(), actual.getPasswordResetTokenExpire());
    assertThat(actual.getPasswordResetToken(), is(not(emptyString())));
    assertThat(diffMinutes, is(1439L)); // 23 hours and 59 minutes

    var request = authResetRequest();
    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
      .queryParam("token", actual.getPasswordResetToken())
    .when()
      .post(AUTH_ENDPOINT + "reset")
    .then()
        .statusCode(200);
  }

  // @endpoint:refresh

  @Test
  void shouldRefreshAccessToken() {
    var request = authRefreshRequest(expiredToken(), "08fa878c-1d28-40d3-a3ef-5a52c649840c");
    given()
      .headers(headers())
      .contentType(JSON)
      .body(request)
    .when()
      .post(AUTH_ENDPOINT + "refresh")
    .then()
        .body("accessToken", is(not(emptyString())))
        .body("refreshToken", is(nullValue()))
        .statusCode(201);
  }

  // @formatter:on
}