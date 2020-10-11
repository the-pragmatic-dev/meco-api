package uk.thepragmaticdev.happy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.when;

import com.stripe.exception.StripeException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import uk.thepragmaticdev.IntegrationConfig;
import uk.thepragmaticdev.IntegrationData;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.kms.usage.ApiKeyUsage;
import uk.thepragmaticdev.kms.usage.ApiKeyUsageCache;
import uk.thepragmaticdev.kms.usage.ApiKeyUsageService;

@ActiveProfiles({ "async-disabled", "http-disabled" })
@Import(IntegrationConfig.class)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiKeyUsageIT extends IntegrationData {

  @LocalServerPort
  private int port;

  @Autowired
  private Clock clock;

  @Autowired
  private ApiKeyUsageCache cache;

  @Autowired
  private ApiKeyUsageService sut;

  /**
   * Called before each integration test to reset database to default state.
   */
  @BeforeEach
  @FlywayTest
  public void initEach() {
  }

  @AfterEach
  public void cleanup() {
    // removes any left over usages in concurrent hash map
    cache.flush();
  }

  @Test
  void shouldReturnEmptyBeforeFlush() {
    var textOps = 10L;
    var imageOps = 3L;
    var apiKeyId = 1L;
    var apiKey = new ApiKey();
    apiKey.setId(apiKeyId);

    cache.put(apiKey, textOps, imageOps);

    var usageDate = LocalDate.of(2020, 02, 01);
    var usages = sut.findByRange(apiKey, usageDate, usageDate);
    assertThat(usages, hasSize(0));
  }

  @Test
  void shouldCreateOneUsageAfterFlush() {
    var usageDate = LocalDate.of(2020, 02, 01);
    mockClock(usageDate);
    var textOps = 10L;
    var imageOps = 3L;
    var apiKeyId = 1L;
    var apiKey = new ApiKey();
    apiKey.setId(apiKeyId);

    cache.put(apiKey, textOps, imageOps);
    cache.flush();

    var usages = sut.findByRange(apiKey, usageDate, usageDate);
    assertThat(usages, hasSize(1));
    assertApiKeyUsage(usages.get(0), apiKey, usageDate, textOps, imageOps);
  }

  @Test
  void shouldUpdateExistingUsageForSameDayAfterFlush() {
    var usageDate = LocalDate.of(2020, 02, 01);
    mockClock(usageDate);
    var textOps = 10;
    var imageOps = 3;
    var apiKey = new ApiKey();
    apiKey.setId(1L);

    cache.put(apiKey, textOps, imageOps);
    cache.put(apiKey, textOps, imageOps);
    cache.flush();

    var usages = sut.findByRange(apiKey, usageDate, usageDate);
    assertThat(usages, hasSize(1));
    assertApiKeyUsage(usages.get(0), apiKey, usageDate, textOps * 2, imageOps * 2);
  }

  @Test
  void shouldCreateFourUsagesForFourDifferentDays() {
    var usageDates = List.of(LocalDate.of(2020, 02, 01), LocalDate.of(2020, 02, 02), LocalDate.of(2020, 02, 03),
        LocalDate.of(2020, 02, 04));
    mockClock(usageDates.toArray(new LocalDate[usageDates.size()]));
    var textOps = 10;
    var imageOps = 3;
    var apiKey = new ApiKey();
    apiKey.setId(1L);

    for (var i = 0; i < usageDates.size(); i++) {
      cache.put(apiKey, textOps, imageOps);
      cache.flush();
    }

    var usages = sut.findByRange(apiKey, usageDates.get(0), usageDates.get(3));
    assertThat(usages, hasSize(4));
    for (var i = 0; i < usages.size(); i++) {
      assertApiKeyUsage(usages.get(i), apiKey, usageDates.get(i), textOps, imageOps);
    }
  }

  @Test
  void shouldCreateOneUsagePerApiKey() {
    var usageDate = LocalDate.of(2020, 02, 01);
    mockClock(usageDate);
    var textOps = 10L;
    var imageOps = 3L;
    var apiKey1 = new ApiKey();
    apiKey1.setId(1L);
    var apiKey2 = new ApiKey();
    apiKey2.setId(2L);

    cache.put(apiKey1, textOps, imageOps);
    cache.put(apiKey2, textOps, imageOps);
    cache.flush();

    var usages1 = sut.findByRange(apiKey1, usageDate, usageDate);
    assertThat(usages1, hasSize(1));
    assertApiKeyUsage(usages1.get(0), apiKey1, usageDate, textOps, imageOps);

    var usages2 = sut.findByRange(apiKey2, usageDate, usageDate);
    assertThat(usages2, hasSize(1));
    assertApiKeyUsage(usages2.get(0), apiKey2, usageDate, textOps, imageOps);
  }

  @Test
  void shouldCreateOneUsagePerApiKeyAndUpdateExistingUsageForSameDay() throws StripeException {
    var usageDate = LocalDate.of(2020, 02, 01);
    mockClock(usageDate);
    var textOps = 10L;
    var imageOps = 3L;
    var apiKey1 = new ApiKey();
    apiKey1.setId(1L);
    var apiKey2 = new ApiKey();
    apiKey2.setId(2L);

    cache.put(apiKey1, textOps, imageOps);
    cache.put(apiKey2, textOps, imageOps);
    cache.put(apiKey2, textOps, imageOps);
    cache.flush();

    var usages1 = sut.findByRange(apiKey1, usageDate, usageDate);
    assertThat(usages1, hasSize(1));
    assertApiKeyUsage(usages1.get(0), apiKey1, usageDate, textOps, imageOps);

    var usages2 = sut.findByRange(apiKey2, usageDate, usageDate);
    assertThat(usages2, hasSize(1));
    assertApiKeyUsage(usages2.get(0), apiKey2, usageDate, textOps * 2, imageOps * 2);
  }

  private void mockClock(LocalDate... dates) {
    var instants = new ArrayList<Instant>();
    for (var localDate : dates) {
      instants.add(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    when(clock.instant()).thenReturn(instants.get(0), instants.subList(1, instants.size()).toArray(new Instant[] {}));
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());
  }

  private void assertApiKeyUsage(ApiKeyUsage usage, ApiKey apiKey, LocalDate usageDate, long textOps, long imageOps) {
    assertThat(usage.getApiKey().getId(), is(apiKey.getId()));
    assertThat(usage.getUsageDate(), is(usageDate));
    assertThat(usage.getTextOperations(), is(textOps));
    assertThat(usage.getImageOperations(), is(imageOps));
  }
}