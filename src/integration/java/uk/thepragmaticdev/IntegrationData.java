package uk.thepragmaticdev;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.specification.ResponseSpecification;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.cactoos.io.ResourceOf;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.dto.request.AccountSigninRequest;
import uk.thepragmaticdev.account.dto.request.AccountSignupRequest;
import uk.thepragmaticdev.account.dto.request.AccountUpdateRequest;
import uk.thepragmaticdev.account.dto.response.AccountSigninResponse;
import uk.thepragmaticdev.kms.AccessPolicy;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.kms.Scope;
import uk.thepragmaticdev.security.request.DeviceMetadata;
import uk.thepragmaticdev.security.request.GeoMetadata;
import uk.thepragmaticdev.security.request.RequestMetadata;

@Component
public abstract class IntegrationData {

  @Autowired
  private ObjectMapper objectMapper;

  protected static final String ACCOUNTS_ENDPOINT = "http://localhost:8080/accounts/";
  protected static final String API_KEY_ENDPOINT = "http://localhost:8080/api-keys/";
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
   * Authorize an account.
   * @return An authentication token
   */
  protected String signin() {
    return String.format("Bearer %s", 
      given()
        .headers(headers())
        .contentType(JSON)
        .body(accountSigninRequest())
      .when()
        .post(ACCOUNTS_ENDPOINT + "signin")
      .then()
          .body("token", not(emptyString()))
          .statusCode(200)
      .extract().as(AccountSigninResponse.class).getToken());
  }

  // @formatter:on

  // @models:default

  protected final Account account() {
    return new Account(null, "admin@email.com", "password", null, null, null, true, false, null, null, null, null,
        null);
  }

  protected final AccountSigninRequest accountSigninRequest() {
    return new AccountSigninRequest("admin@email.com", "password");
  }

  protected final AccountSignupRequest accountSignupRequest() {
    return new AccountSignupRequest("admin@email.com", "password");
  }

  protected final AccountUpdateRequest accountUpdateRequest() {
    return new AccountUpdateRequest("Ash", false, true);
  }

  protected final ApiKey key() {
    return new ApiKey(null, "name", null, null, null, null, null, null, true, scope(), Arrays.asList(accessPolicy()),
        null, null);
  }

  protected final AccessPolicy accessPolicy() {
    return new AccessPolicy(null, "name", "127.0.0.1/32", null);
  }

  protected final Scope scope() {
    return new Scope(null, true, true, false, false, null);
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

  // @models:dirty

  protected final Account dirtyAccount() {
    return podam(Account.class);
  }

  protected final ApiKey dirtyKey() {
    var dirtyKey = podam(ApiKey.class);
    dirtyKey.setAccessPolicies(Arrays.asList(dirtyAccessPolicy()));
    return dirtyKey;
  }

  protected final AccessPolicy dirtyAccessPolicy() {
    var dirtyAccessPolicy = podam(AccessPolicy.class);
    dirtyAccessPolicy.setRange("89.1.2.3/32");
    return dirtyAccessPolicy;
  }

  private <T> T podam(Class<T> pojoClass) {
    var factory = new PodamFactoryImpl();
    factory.getStrategy().setDefaultNumberOfCollectionElements(0);
    factory.getStrategy().addOrReplaceTypeManufacturer(Date.class, new DateManufacturer());
    return factory.manufacturePojo(pojoClass);
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
}
