package uk.thepragmaticdev.email;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.email.EmailProperties.From;
import uk.thepragmaticdev.email.EmailProperties.Template;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.security.request.DeviceMetadata;
import uk.thepragmaticdev.security.request.GeoMetadata;
import uk.thepragmaticdev.security.request.RequestMetadata;

@SpringBootTest
class EmailServiceTest {

  private static MockWebServer mockBackEnd;

  @Mock
  private EmailProperties emailProperties;

  private EmailService sut;

  @BeforeAll
  public static void setUp() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start();
  }

  @AfterAll
  public static void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  /**
   * Called before each test. Builds the system under test and stubs email
   * property endpoints.
   */
  @BeforeEach
  public void initEach() {
    var mockFrom = mock(From.class);
    when(mockFrom.getName()).thenReturn("name");
    when(emailProperties.getUrl()).thenReturn(String.format("http://localhost:%s", mockBackEnd.getPort()));
    when(emailProperties.getDomain()).thenReturn("");
    when(emailProperties.getSecretKey()).thenReturn("secret");
    when(emailProperties.getFrom()).thenReturn(mockFrom);
    when(emailProperties.getTemplates()).thenReturn(validTemplates());
    sut = new EmailService(emailProperties, WebClient.builder());
  }

  @Test()
  void shouldSendAccountCreatedEmail() throws InterruptedException, UnsupportedEncodingException {
    var username = "ash@ketchum.com";
    var account = mock(Account.class);
    when(account.getUsername()).thenReturn(username);

    mockBackEnd.enqueue(new MockResponse());
    sut.sendAccountCreated(account);
    var request = mockBackEnd.takeRequest();

    assertQueryParams(request, username, "subject1", "account-created");
  }

  @Test()
  void shouldSendForgotPasswordEmail() throws InterruptedException, UnsupportedEncodingException {
    var username = "ash@ketchum.com";
    var token = "token";
    var account = mock(Account.class);
    when(account.getUsername()).thenReturn(username);
    when(account.getPasswordResetToken()).thenReturn(token);

    mockBackEnd.enqueue(new MockResponse());
    sut.sendForgottenPassword(account);
    var request = mockBackEnd.takeRequest();

    assertQueryParams(request, username, "subject2", "account-forgotten-password");
    assertBody(request, List.of("v:username=".concat(username), "v:token=".concat(token)));
  }

  @Test()
  void shouldSendResetEmail() throws InterruptedException, UnsupportedEncodingException {
    var username = "ash@ketchum.com";
    var account = mock(Account.class);
    when(account.getUsername()).thenReturn(username);

    mockBackEnd.enqueue(new MockResponse());
    sut.sendResetPassword(account);
    var request = mockBackEnd.takeRequest();

    assertQueryParams(request, username, "subject3", "account-reset-password");
    assertBody(request, List.of("v:username=".concat(username)));
  }

  @Test()
  void shouldUnrecognizedDeviceEmail() throws InterruptedException, UnsupportedEncodingException {
    var username = "ash@ketchum.com";
    var account = mock(Account.class);
    when(account.getUsername()).thenReturn(username);

    var log = mock(SecurityLog.class);
    var metadata = mockRequestMetadata();
    when(log.getCreatedDate()).thenReturn(OffsetDateTime.now());
    when(log.getRequestMetadata()).thenReturn(metadata);

    mockBackEnd.enqueue(new MockResponse());
    sut.sendUnrecognizedDevice(account, log);
    var request = mockBackEnd.takeRequest();

    assertQueryParams(request, username, "subject4", "account-unrecognized-device");
    assertBody(request, List.of(//
        "v:time=".concat(log.getCreatedDate().toString()), //
        "v:cityName=".concat(log.getRequestMetadata().getGeoMetadata().getCityName()), //
        "v:countryIsoCode=".concat(log.getRequestMetadata().getGeoMetadata().getCountryIsoCode()), //
        "v:subdivisionIsoCode=".concat(log.getRequestMetadata().getGeoMetadata().getSubdivisionIsoCode()), //
        "v:operatingSystemFamily=".concat(log.getRequestMetadata().getDeviceMetadata().getOperatingSystemFamily()),
        "v:userAgentFamily=".concat(log.getRequestMetadata().getDeviceMetadata().getUserAgentFamily()) //
    ));
  }

  @Test()
  void shouldSendKeyCreatedEmail() throws InterruptedException, UnsupportedEncodingException {
    var username = "ash@ketchum.com";
    var account = mock(Account.class);
    when(account.getUsername()).thenReturn(username);

    var key = mock(ApiKey.class);
    when(key.getName()).thenReturn("keyname");
    when(key.getPrefix()).thenReturn("prefix");

    mockBackEnd.enqueue(new MockResponse());
    sut.sendKeyCreated(account, key);
    var request = mockBackEnd.takeRequest();

    assertQueryParams(request, username, "subject5", "key-created");
    assertBody(request, List.of("v:name=".concat(key.getName()), "v:prefix=".concat(key.getPrefix())));
  }

  @Test()
  void shouldSendKeyDeletedEmail() throws InterruptedException, UnsupportedEncodingException {
    var username = "ash@ketchum.com";
    var account = mock(Account.class);
    when(account.getUsername()).thenReturn(username);

    var key = mock(ApiKey.class);
    when(key.getName()).thenReturn("keyname");
    when(key.getPrefix()).thenReturn("prefix");

    mockBackEnd.enqueue(new MockResponse());
    sut.sendKeyDeleted(account, key);
    var request = mockBackEnd.takeRequest();

    assertQueryParams(request, username, "subject6", "key-deleted");
    assertBody(request, List.of("v:name=".concat(key.getName()), "v:prefix=".concat(key.getPrefix())));
  }

  private void assertQueryParams(RecordedRequest request, String to, String subject, String template)
      throws UnsupportedEncodingException {
    var parameters = UriComponentsBuilder.fromUri(request.getRequestUrl().uri()).build().getQueryParams();
    var fromParam = URLDecoder.decode(parameters.get("from").get(0), StandardCharsets.UTF_8.name());
    assertThat(request.getMethod(), is("POST"));
    assertThat(request.getPath(), startsWith("/messages?"));
    assertThat(fromParam, is("name <mailgun@.mailgun.org>"));
    assertThat(parameters.get("to").get(0), is(to));
    assertThat(parameters.get("subject").get(0), is(subject));
    assertThat(parameters.get("template").get(0), is(template));
  }

  private void assertBody(RecordedRequest request, List<String> formData) throws UnsupportedEncodingException {
    var body = URLDecoder.decode(request.getBody().readUtf8(), StandardCharsets.UTF_8.name()).split("&");
    assertThat(body, arrayContaining(formData.toArray()));
  }

  private RequestMetadata mockRequestMetadata() {
    var metadata = mock(RequestMetadata.class);
    var geo = mockGeoMetadata();
    var device = mockDeviceMetadata();
    when(metadata.getGeoMetadata()).thenReturn(geo);
    when(metadata.getDeviceMetadata()).thenReturn(device);
    return metadata;
  }

  private GeoMetadata mockGeoMetadata() {
    var geo = mock(GeoMetadata.class);
    when(geo.getCityName()).thenReturn("city");
    when(geo.getCountryIsoCode()).thenReturn("country");
    when(geo.getSubdivisionIsoCode()).thenReturn("sub");
    return geo;
  }

  private DeviceMetadata mockDeviceMetadata() {
    var device = mock(DeviceMetadata.class);
    when(device.getOperatingSystemFamily()).thenReturn("os");
    when(device.getOperatingSystemMajor()).thenReturn("1");
    when(device.getOperatingSystemMinor()).thenReturn("2");
    when(device.getUserAgentFamily()).thenReturn("ua");
    when(device.getUserAgentMajor()).thenReturn("3");
    when(device.getUserAgentMinor()).thenReturn("4");
    return device;
  }

  private List<Template> validTemplates() {
    return List.of(//
        new Template("account-created", "subject1"), //
        new Template("account-forgotten-password", "subject2"), //
        new Template("account-reset-password", "subject3"), //
        new Template("account-unrecognized-device", "subject4"), //
        new Template("key-created", "subject5"), //
        new Template("key-deleted", "subject6"));
  }
}