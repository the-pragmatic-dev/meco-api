package uk.thepragmaticdev;

import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.factory.Jackson2ObjectMapperFactory;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.thepragmaticdev.account.Account;

@Component
public abstract class IntegrationData {

  @Autowired
  private ObjectMapper objectMapper;

  protected static final String AUTH_HEADER = "Authorization";
  protected static final String ACCOUNTS_ENDPOINT = "http://localhost:8080/accounts/";

  /**
   * Default to logging RestAssured errors only if validation fails and disable
   * Jackson annotations to serialise test data.
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

  // Default models used for integration tests.

  public final Account account() {
    return new Account(null, "admin@email.com", "password", null, true, false, null, null, null);
  }

  // Dirty models used for integration tests.

  public final Account dirtyAccount() {
    return podam(Account.class);
  }

  private <T> T podam(Class<T> pojoClass) {
    PodamFactory factory = new PodamFactoryImpl();
    factory.getStrategy().setDefaultNumberOfCollectionElements(0);
    factory.getStrategy().addOrReplaceTypeManufacturer(Date.class, new DateManufacturer());
    return factory.manufacturePojo(pojoClass);
  }

  // Formatting helpers.

  public String format(Date dateToFormat) {
    return new SimpleDateFormat("yyy-MM-dd").format(dateToFormat);
  }
}
