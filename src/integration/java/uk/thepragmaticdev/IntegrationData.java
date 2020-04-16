package uk.thepragmaticdev;

import static io.restassured.RestAssured.given;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.factory.Jackson2ObjectMapperFactory;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import org.cactoos.io.ResourceOf;
import org.cactoos.text.FormattedText;
import org.cactoos.text.TextOf;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.kms.AccessPolicy;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.kms.Scope;

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

  // @formatter:off

  /**
   * Authorize an account.
   * @return An authentication token
   */
  protected String signin() {
    return String.format("Bearer %s", 
      given()
        .contentType(JSON)
        .body(account())
      .when()
        .post(ACCOUNTS_ENDPOINT + "signin")
      .then()
          .body(not(emptyString()))
          .statusCode(200)
      .extract().body().asString());
  }

  // @formatter:on

  // @models:default

  protected final Account account() {
    return new Account(null, "admin@email.com", "password", null, true, false, null, null, null);
  }

  protected final ApiKey key() {
    return new ApiKey(null, "name", null, null, null, null, null, null, true, scope(), Arrays.asList(accessPolicy()),
        null);
  }

  protected final AccessPolicy accessPolicy() {
    return new AccessPolicy(null, "name", "127.0.0.1/32", null);
  }

  protected final Scope scope() {
    return new Scope(null, true, true, false, false, null);
  }

  // @models:dirty

  protected final Account dirtyAccount() {
    return podam(Account.class);
  }

  protected final ApiKey dirtyKey() {
    ApiKey dirtyKey = podam(ApiKey.class);
    dirtyKey.setAccessPolicies(Arrays.asList(dirtyAccessPolicy()));
    return dirtyKey;
  }

  protected final AccessPolicy dirtyAccessPolicy() {
    AccessPolicy dirtyAccessPolicy = podam(AccessPolicy.class);
    dirtyAccessPolicy.setRange("89.1.2.3/32");
    return dirtyAccessPolicy;
  }

  private <T> T podam(Class<T> pojoClass) {
    PodamFactory factory = new PodamFactoryImpl();
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

      private OffsetDateTime now;

      @Override
      public boolean matches(Object actual) {
        long diff = Math.abs(unit.between(OffsetDateTime.parse((CharSequence) actual), now));
        return diff <= amount;
      }

      @Override
      public void describeTo(Description description) {
        now = OffsetDateTime.now();
        description
            .appendText(String.format("date to be within last %d %s of %s", amount, unit.toString(), now.toString()));
      }
    };
  }
}
