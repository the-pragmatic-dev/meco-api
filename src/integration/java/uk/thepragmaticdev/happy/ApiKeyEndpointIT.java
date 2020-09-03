package uk.thepragmaticdev.happy;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.kms.dto.response.ApiKeyCreateResponse;

@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
class ApiKeyEndpointIT extends IntegrationData {
  // @formatter:off

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  // @endpoint:findAll

  @Test
  void shouldReturnAllKeysOwnedByAuthenticatedAccount() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT)
    .then()
        .body("$", hasSize(2))
        // first key
        .rootPath("get(0)")
          .body("id", is(1))
          .body("name", is("Good Coffee Shop"))
          .body("prefix", is("rAosN1E"))
          .body("key", is(nullValue()))
          .body("created_date", is("2020-02-25T13:38:58.232Z"))
          .body("last_used_date", is(nullValue()))
          .body("modified_date", is("2020-02-25T13:40:19.111Z"))
          .body("enabled", is(true))
          .body("access_policies", hasSize(2))
          .rootPath("get(0).scope")
            .body("image", is(false))
            .body("gif", is(true))
            .body("text", is(true))
            .body("video", is(false))
          .rootPath("get(0).access_policies.get(0)")
            .body("name", is("newcastle"))
            .body("range", is("5.65.196.0/16"))
          .rootPath("get(0).access_policies.get(1)")
            .body("name", is("quedgeley"))
            .body("range", is("17.22.136.0/32"))
        // second key
        .rootPath("get(1)")
          .body("id", is(2))
          .body("name", is("Bobs Pastry Shop"))
          .body("prefix", is("7Cx9VYK"))
          .body("key", is(nullValue()))
          .body("created_date", is("2020-02-25T15:06:41.718Z"))
          .body("last_used_date", is(nullValue()))
          .body("modified_date", is(nullValue()))
          .body("enabled", is(true))
          .body("access_policies", hasSize(0))
          .rootPath("get(1).scope")
            .body("image", is(true))
            .body("gif", is(false))
            .body("text", is(false))
            .body("video", is(true))
        .statusCode(200);
  }

  // @endpoint:findById

  @Test
  void shouldReturnKeyOwnedByAuthenticatedAccount() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "1")
    .then()
        .body("id", is(1))
        .body("name", is("Good Coffee Shop"))
        .body("prefix", is("rAosN1E"))
        .body("key", is(nullValue()))
        .body("created_date", is("2020-02-25T13:38:58.232Z"))
        .body("last_used_date", is(nullValue()))
        .body("modified_date", is("2020-02-25T13:40:19.111Z"))
        .body("enabled", is(true))
        .body("access_policies", hasSize(2))
        .rootPath("scope")
          .body("image", is(false))
          .body("gif", is(true))
          .body("text", is(true))
          .body("video", is(false))
        .rootPath("access_policies.get(0)")
          .body("name", is("newcastle"))
          .body("range", is("5.65.196.0/16"))
        .rootPath("access_policies.get(1)")
          .body("name", is("quedgeley"))
          .body("range", is("17.22.136.0/32"))
        .statusCode(200);
  }

  // @endpoint:create

  @Test
  void shouldCreateKey() {
    var request = apiKeyCreateRequest();
    var response = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin())
          .contentType(JSON)
          .body(request)
        .when()
          .post(API_KEY_ENDPOINT)
        .then()
          .body("id", is(3))
          .body("name", is("name"))
          .body("prefix.length()", is(7))
          .body("key", containsString("."))
          .body("created_date", is(withinLast(5, ChronoUnit.SECONDS)))
          .body("last_used_date", is(nullValue()))
          .body("modified_date", is(nullValue()))
          .body("enabled", is(true))
          .body("scope.image", is(true))
          .body("scope.gif", is(true))
          .body("scope.text", is(false))
          .body("scope.video", is(false))
          .body("access_policies.size()", is(1))
          .rootPath("access_policies[0]")
            .body("name", is("name"))
            .body("range", is("127.0.0.1/32"))
          .statusCode(201)
        .extract().as(ApiKeyCreateResponse.class);
    assertValidKey(response);
  }

  @Test
  void shouldCreateKeyWithDefaultValues() {
    var request = apiKeyCreateRequest();
    request.setEnabled(null);
    request.setScope(null);
    request.setAccessPolicies(null);
    var response = given()
          .headers(headers())
          .header(HttpHeaders.AUTHORIZATION, signin())
          .contentType(JSON)
          .body(request)
        .when()
          .post(API_KEY_ENDPOINT)
        .then()
          .body("id", is(3))
          .body("name", is("name"))
          .body("prefix.length()", is(7))
          .body("key", containsString("."))
          .body("created_date", is(withinLast(5, ChronoUnit.SECONDS)))
          .body("last_used_date", is(nullValue()))
          .body("modified_date", is(nullValue()))
          .body("enabled", is(false))
          .body("scope.image", is(false))
          .body("scope.gif", is(false))
          .body("scope.text", is(false))
          .body("scope.video", is(false))
          .body("access_policies.size()", is(0))
          .statusCode(201)
        .extract().as(ApiKeyCreateResponse.class);
    assertValidKey(response);
  }

  // @endpoint:update

  @Test
  void shouldUpdateOnlyMutableKeyFields() {
    var request = apiKeyUpdateRequest();
    given()
      .contentType(JSON)
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
      .then()
        .body("id", is(1))
        .body("name", is(request.getName()))
        .body("prefix", is("rAosN1E"))
        .body("key", is(nullValue()))
        .body("created_date", is("2020-02-25T13:38:58.232Z"))
        .body("last_used_date", is(nullValue()))
        .body("modified_date", is(withinLast(5, ChronoUnit.SECONDS)))
        .body("enabled", is(request.getEnabled()))
        .body("scope.image", is(request.getScope().getImage()))
        .body("scope.gif", is(request.getScope().getGif()))
        .body("scope.text", is(request.getScope().getText()))
        .body("scope.video", is(request.getScope().getVideo()))
        .body("access_policies.size()", is(request.getAccessPolicies().size()))
        .rootPath("access_policies[0]")
          .body("name", is(request.getAccessPolicies().get(0).getName()))
          .body("range", is(request.getAccessPolicies().get(0).getRange()))
        .statusCode(200);
  }

  @Test
  void shouldUpdateOnlyNonNullKeyFields() {
    var request = apiKeyUpdateRequest();
    request.setEnabled(null);
    request.setScope(null);
    request.setAccessPolicies(null);
    given()
      .contentType(JSON)
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
      .body(request)
    .when()
      .put(API_KEY_ENDPOINT + "1")
      .then()
        .body("id", is(1))
        .body("name", is(request.getName()))
        .body("prefix", is("rAosN1E"))
        .body("key", is(nullValue()))
        .body("created_date", is("2020-02-25T13:38:58.232Z"))
        .body("last_used_date", is(nullValue()))
        .body("modified_date", is(withinLast(5, ChronoUnit.SECONDS)))
        .body("enabled", is(true))
        .body("scope.image", is(false))
        .body("scope.gif", is(true))
        .body("scope.text", is(true))
        .body("scope.video", is(false))
        .body("access_policies.size()", is(2))
        .rootPath("access_policies[0]")
          .body("name", is("newcastle"))
          .body("range", is("5.65.196.0/16"))
        .rootPath("access_policies[1]")
          .body("name", is("quedgeley"))
          .body("range", is("17.22.136.0/32"))
        .statusCode(200);
  }

  // @endpoint:delete

  @Test
  void shouldDeleteKey() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .delete(API_KEY_ENDPOINT + "1")
    .then()
        .body(is(emptyString()))
        .statusCode(204);
    // check count has reduced
    given()
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "count")
      .then()
        .body(is("1"))
        .statusCode(200);
  }

  // @endpoint:key-logs

  @Test
  void shouldReturnLatestLogsForKey() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "1/logs")
    .then()
        .body("number_of_elements", is(3))
        .body("content", hasSize(3))
        .rootPath("content[0]")
          .body("action", is("text.predict"))
          .body("created_date", is("2020-02-26T15:40:19.111Z"))
          .spec(validRequestMetadataSpec(0))
        .rootPath("content[1]")
          .body("action", is("gif.predict"))
          .body("created_date", is("2020-02-25T15:40:19.111Z"))
          .spec(validRequestMetadataSpec(1))
        .rootPath("content[2]")
          .body("action", is("image.predict"))
          .body("created_date", is("2020-02-24T15:40:19.111Z"))
          .spec(validRequestMetadataSpec(2))
        .statusCode(200);
  }

  // @endpoint:key-logs-download

  @Test
  void shouldDownloadKeyLogs() throws IOException {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "1/logs/download")
    .then()
        .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, is(HttpHeaders.CONTENT_DISPOSITION))
        .header(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment; filename="))
        .body(is(csv("data/key.log.csv")))
        .statusCode(200);
  }

  // @endpoint:count

  @Test
  void shouldReturnKeyCountOfAuthenticatedAccount() {
    given()
      .headers(headers())
      .header(HttpHeaders.AUTHORIZATION, signin())
    .when()
      .get(API_KEY_ENDPOINT + "count")
      .then()
        .body(is("2"))
        .statusCode(200);
  }
  
  private void assertValidKey(ApiKeyCreateResponse response) {
    var key = response.getKey();
    assertThat(key, startsWith(String.format("%s.", response.getPrefix())));
    assertThat(key.substring(key.lastIndexOf(".") + 1).length(), is(48));
  }

  // @formatter:on
}