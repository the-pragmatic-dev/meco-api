package uk.thepragmaticdev;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.specification.ResponseSpecification;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cactoos.io.ResourceOf;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.thepragmaticdev.account.dto.request.AccountUpdateRequest;
import uk.thepragmaticdev.auth.dto.request.AuthRefreshRequest;
import uk.thepragmaticdev.auth.dto.request.AuthResetRequest;
import uk.thepragmaticdev.auth.dto.request.AuthSigninRequest;
import uk.thepragmaticdev.auth.dto.request.AuthSignupRequest;
import uk.thepragmaticdev.auth.dto.response.AuthSigninResponse;
import uk.thepragmaticdev.billing.dto.request.BillingCreateSubscriptionRequest;
import uk.thepragmaticdev.kms.dto.request.AccessPolicyRequest;
import uk.thepragmaticdev.kms.dto.request.ApiKeyCreateRequest;
import uk.thepragmaticdev.kms.dto.request.ApiKeyUpdateRequest;
import uk.thepragmaticdev.kms.dto.request.ScopeRequest;
import uk.thepragmaticdev.security.request.DeviceMetadata;
import uk.thepragmaticdev.security.request.GeoMetadata;
import uk.thepragmaticdev.security.request.RequestMetadata;
import uk.thepragmaticdev.text.dto.request.TextRequest;

@Component
public abstract class IntegrationData {

  @Autowired
  private ObjectMapper objectMapper;

  protected static final String AUTH_ENDPOINT = "http://localhost:8080/v1/auth/";
  protected static final String ACCOUNTS_ENDPOINT = "http://localhost:8080/v1/accounts/";
  protected static final String API_KEY_ENDPOINT = "http://localhost:8080/v1/api-keys/";
  protected static final String BILLING_ENDPOINT = "http://localhost:8080/v1/billing/";
  protected static final String TEXT_ENDPOINT = "http://localhost:8080/v1/text/";
  protected static final String INVALID_TOKEN = "Bearer invalidToken";

  /**
   * Default to logging RestAssured errors only if validation fails. Also disable
   * Jackson annotations to enable serialising test data.
   */
  public IntegrationData() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    RestAssured.config = RestAssuredConfig.config()
        .objectMapperConfig(objectMapperConfig().jackson2ObjectMapperFactory(new Jackson2ObjectMapperFactory() {
          @Override
          public ObjectMapper create(Type cls, String charset) {
            objectMapper.disable(MapperFeature.USE_ANNOTATIONS);
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            return objectMapper;
          }
        }));
  }

  /**
   * Default headers to mock user agent and ip.
   * 
   * @return A map of default headers
   */
  protected Map<String, String> headers() {
    var headers = new HashMap<String, String>();
    headers.put("User-Agent", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_0)"
        + " AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
    headers.put("X-FORWARDED-FOR", "196.245.163.202");
    return headers;
  }

  protected ResponseSpecification validRequestMetadataSpec(int index) {
    var root = String.format("content[%d].request_metadata.", index);
    var builder = new ResponseSpecBuilder();
    builder.expectBody(root.concat("ip"), is("196.245.163.202"));
    builder.rootPath(root.concat("geo_metadata"));
    builder.expectBody("city_name", is("London"));
    builder.expectBody("country_iso_code", is("GB"));
    builder.expectBody("subdivision_iso_code", is("ENG"));
    builder.rootPath(root.concat("device_metadata"));
    builder.expectBody("operating_system_family", is("Mac OS X"));
    builder.expectBody("operating_system_major", is("10"));
    builder.expectBody("operating_system_minor", is("14"));
    builder.expectBody("user_agent_family", is("Chrome"));
    builder.expectBody("user_agent_major", is("71"));
    builder.expectBody("user_agent_minor", is("0"));
    return builder.build();
  }

  // @formatter:off

  /**
   * Create an api key header value with default credentials.
   * @return An api key header value
   */
  protected String apiKey() {
    return apiKey("rAosN1E.OGE0NDU3NDUtYTcyNS00Y2U3LWE2M2UtMzY2NzI2OGJhNzBh");
  }

  /**
   * Create an api key header value with given credentials.
   * @return An api key header value
   */
  protected String apiKey(String rawKey) {
    return String.format("ApiKey %s", rawKey);
  }

  /**
   * Authorize an account with default credentials.
   * @return An authentication token
   */
  protected String signin() {
    return signin(authSigninRequest());
  }

  /**
   * Authorize an account with given credentials.
   * @return An authentication token
   */
  protected String signin(AuthSigninRequest request) {
    return token(
      given()
        .headers(headers())
        .contentType(JSON)
        .body(request)
      .when()
        .post(AUTH_ENDPOINT + "signin")
      .then()
          .body("access_token", is(not(emptyString())))
          .body("refresh_token", is(not(emptyString())))
          .statusCode(200)
      .extract().as(AuthSigninResponse.class).getAccessToken());
  }

  private String token(String token) {
    return String.format("Bearer %s", token);
  }

  // @formatter:on

  // @models:default

  protected final AuthSigninRequest authSigninRequest() {
    return authSigninRequest("admin@email.com", "password");
  }

  protected final AuthSigninRequest authSigninRequest(String username, String password) {
    return new AuthSigninRequest(username, password);
  }

  protected final AuthSignupRequest authSignupRequest() {
    return authSignupRequest("admin@email.com", "password");
  }

  protected final AuthSignupRequest authSignupRequest(String username, String password) {
    return new AuthSignupRequest(username, password);
  }

  protected final AccountUpdateRequest accountUpdateRequest() {
    return new AccountUpdateRequest("Ash", false, true);
  }

  protected final AuthResetRequest authResetRequest() {
    return new AuthResetRequest("newpassword");
  }

  protected final AuthRefreshRequest authRefreshRequest(String accessToken, String refreshToken) {
    return new AuthRefreshRequest(accessToken, refreshToken);
  }

  protected final ApiKeyCreateRequest apiKeyCreateRequest() {
    return new ApiKeyCreateRequest("name", true, scopeRequest(true, true, false, false),
        List.of(accessPolicyRequest("name", "127.0.0.1/32")));
  }

  protected final ApiKeyUpdateRequest apiKeyUpdateRequest() {
    return new ApiKeyUpdateRequest("newname", false, scopeRequest(false, false, true, true),
        List.of(accessPolicyRequest("newname", "66.0.0.1/16")));
  }

  protected final ScopeRequest scopeRequest(boolean image, boolean gif, boolean text, boolean video) {
    return new ScopeRequest(image, gif, text, video);
  }

  protected final AccessPolicyRequest accessPolicyRequest(String name, String range) {
    return new AccessPolicyRequest(name, range);
  }

  protected final RequestMetadata requestMetadata() {
    return new RequestMetadata("196.245.163.202", geoMetadata(), deviceMetadata());
  }

  protected final GeoMetadata geoMetadata() {
    return new GeoMetadata("London", "GB", "ENG");
  }

  protected final DeviceMetadata deviceMetadata() {
    return new DeviceMetadata("Mac OS X", "10", "14", "Chrome", "71", "0");
  }

  protected final BillingCreateSubscriptionRequest billingCreateSubscriptionRequest(String price) {
    return new BillingCreateSubscriptionRequest(price);
  }

  protected final TextRequest textRequest() {
    return new TextRequest("a very stupid comment");
  }

  protected final TextRequest textRequest(String text) {
    return new TextRequest(text);
  }

  // @helpers:formatting

  protected String csv(String file) throws IOException {
    return new FormattedText(new TextOf(new ResourceOf(file))).asString();
  }

  // @helpers:matchers

  protected Matcher<String> withinLast(final int amount, final ChronoUnit unit) {
    return new BaseMatcher<String>() {

      @Override
      public boolean matches(Object actual) {
        var diff = Math.abs(unit.between(OffsetDateTime.parse((CharSequence) actual), OffsetDateTime.now()));
        return diff <= amount;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText(String.format("date to be within last %d %s of %s", amount, unit.toString(),
            OffsetDateTime.now().toString()));
      }
    };
  }

  // @helpers:tokens

  protected String futureToken() {
    // generated with an expiration date of 30th September 3921
    return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlbWFpbC5jb20iLCJhdXRoIjpbeyJhdX"
        + "Rob3JpdHkiOiJST0xFX0FETUlOIn1dLCJpYXQiOjE1OTAzMTcxNTMsImV4cCI6NjE1OTAzMT"
        + "cwOTN9.DX6oWfZ1tU3l7gssicEQfcEzyXQqphNyxwBnVcnSsaI";
  }

  protected String expiredToken() {
    // generated with an expiration date of 24th May 2020
    return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlbWFpbC5jb20iLCJhdXRoIjpbeyJhdX"
        + "Rob3JpdHkiOiJST0xFX0FETUlOIn1dLCJpYXQiOjE1OTAzMTQ5MjQsImV4cCI6MTU5MDMxNT"
        + "IyNH0.dPoby2Rew0Imq3giTk-K2uI_BG35HqkWJn43HU2iaIk";
  }

  protected String incorrectSignatureToken() {
    // generated with an incorrect signing key
    return "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbkBlbWFpbC5jb20iLCJhdXRoIjpbeyJhdX"
        + "Rob3JpdHkiOiJST0xFX0FETUlOIn1dLCJpYXQiOjE1OTAzMzA3MDYsImV4cCI6MTU5MDMzMT"
        + "AwNn0.F0sH-eM1vKlYgUO1ehwL-nxrQMQBRuQsK9WtUj2LvOI";
  }
}
