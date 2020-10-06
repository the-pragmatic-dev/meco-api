package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.exception.code.SecurityCode;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
    FlywayTestExecutionListener.class })
@SpringBootTest(properties = { "bucket.capacity=3", "bucket.tokens=3" }, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RateLimitIT extends IntegrationData {

  @LocalServerPort
  private int port;

  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  // @endpoint:me

  @Test
  void shouldReturnTooManyRequestsWhenRateLimitIsHit() {
    var request = authSigninRequest();
    var maxLimit = 3;

    for (int i = 0; i < maxLimit; i++) {
      given()
        .headers(headers())
        .contentType(JSON)
        .body(request)
      .when()
        .post(authEndpoint(port) + "signin")
      .then()
          .header("X-Rate-Limit-Remaining", is(Long.toString(maxLimit - (i + 1))))
          .statusCode(200);
    }

    given()
        .headers(headers())
        .contentType(JSON)
        .body(request)
      .when()
        .post(authEndpoint(port) + "signin")
      .then()
          .header("X-Rate-Limit-Retry-After-Seconds", Integer::parseInt, greaterThan(50))
          .body("status", is("TOO_MANY_REQUESTS"))
          .body("message", is(SecurityCode.TOO_MANY_REQUESTS.getMessage()))
          .statusCode(429);
  }
}
