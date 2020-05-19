package uk.thepragmaticdev;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.factory.Jackson2ObjectMapperFactory;
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
import uk.thepragmaticdev.account.dto.request.AccountResetRequest;
import uk.thepragmaticdev.account.dto.request.AccountSigninRequest;
import uk.thepragmaticdev.account.dto.request.AccountSignupRequest;
import uk.thepragmaticdev.account.dto.request.AccountUpdateRequest;
import uk.thepragmaticdev.account.dto.response.AccountSigninResponse;
import uk.thepragmaticdev.billing.dto.request.BillingCreateSubscriptionRequest;
import uk.thepragmaticdev.kms.dto.request.AccessPolicyRequest;
import uk.thepragmaticdev.kms.dto.request.ApiKeyCreateRequest;
import uk.thepragmaticdev.kms.dto.request.ApiKeyUpdateRequest;
import uk.thepragmaticdev.kms.dto.request.ScopeRequest;
import uk.thepragmaticdev.security.request.DeviceMetadata;
import uk.thepragmaticdev.security.request.GeoMetadata;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Component
public abstract class IntegrationData {

  @Autowired
  private ObjectMapper objectMapper;

  protected static final String ACCOUNTS_ENDPOINT = "http://localhost:8080/accounts/";
  protected static final String API_KEY_ENDPOINT = "http://localhost:8080/api-keys/";
  protected static final String BILLING_ENDPOINT = "http://localhost:8080/billing/";
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
    var root = String.format("content[%d].requestMetadata.", index);
    var builder = new ResponseSpecBuilder();
    builder.expectBody(root.concat("ip"), is("196.245.163.202"));
    builder.rootPath(root.concat("geoMetadata"));
    builder.expectBody("cityName", is("London"));
    builder.expectBody("countryIsoCode", is("GB"));
    builder.expectBody("subdivisionIsoCode", is("ENG"));
    builder.rootPath(root.concat("deviceMetadata"));
    builder.expectBody("operatingSystemFamily", is("Mac OS X"));
    builder.expectBody("operatingSystemMajor", is("10"));
    builder.expectBody("operatingSystemMinor", is("14"));
    builder.expectBody("userAgentFamily", is("Chrome"));
    builder.expectBody("userAgentMajor", is("71"));
    builder.expectBody("userAgentMinor", is("0"));
    return builder.build();
  }

  // @formatter:off

  /**
   * Authorize an account with default credentials.
   * @return An authentication token
   */
  protected String signin() {
    return signin(accountSigninRequest());
  }

  /**
   * Authorize an account with given credentials.
   * @return An authentication token
   */
  protected String signin(AccountSigninRequest request) {
    return token(
      given()
        .headers(headers())
        .contentType(JSON)
        .body(request)
      .when()
        .post(ACCOUNTS_ENDPOINT + "signin")
      .then()
          .body("token", not(emptyString()))
          .statusCode(200)
      .extract().as(AccountSigninResponse.class).getToken());
  }

  private String token(String token) {
    return String.format("Bearer %s", token);
  }

  // @formatter:on

  // @models:default

  protected final AccountSigninRequest accountSigninRequest() {
    return accountSigninRequest("admin@email.com", "password");
  }

  protected final AccountSigninRequest accountSigninRequest(String username, String password) {
    return new AccountSigninRequest(username, password);
  }

  protected final AccountSignupRequest accountSignupRequest() {
    return accountSignupRequest("admin@email.com", "password");
  }

  protected final AccountSignupRequest accountSignupRequest(String username, String password) {
    return new AccountSignupRequest(username, password);
  }

  protected final AccountUpdateRequest accountUpdateRequest() {
    return new AccountUpdateRequest("Ash", false, true);
  }

  protected final AccountResetRequest accountResetRequest() {
    return new AccountResetRequest("newpassword");
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

  // @helpers:clean-up

  protected void deleteStripeCustomer(String stripeCustomerId) throws StripeException {
    Customer.retrieve(stripeCustomerId).delete();
  }
}
