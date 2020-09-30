package uk.thepragmaticdev.security.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.log.security.SecurityLogService;

@ExtendWith(MockitoExtension.class)
class RequestMetadataServiceTest {

  @Mock
  private GeoMetadataService geoMetadataService;

  @Mock
  private EmailService emailService;

  @Mock
  private SecurityLogService securityLogService;

  @Mock
  private DatabaseReaderFactory factory;

  @Mock
  private DatabaseReader reader;

  private RequestMetadataService sut;

  /**
   * Called before each test. Builds the system under test.
   * 
   * @throws IOException If database path does not exist
   */
  @BeforeEach
  public void initEach() throws IOException {
    sut = new RequestMetadataService("database.mmdb", "url", "src/test/resources/geolite", geoMetadataService,
        emailService, securityLogService, factory);
  }

  @Test
  void shouldReturnFalseIfDatabaseFileDoesNotExist() throws IOException {
    when(factory.create(any(Path.class))).thenThrow(IOException.class);
    boolean loaded = sut.loadDatabase();
    assertThat(loaded, is(false));
  }

  @Test
  void shouldReturnEmptyRequestMetadataIfGeoIp2ExceptionOccurs() throws IOException, GeoIp2Exception {
    when(factory.create(any(Path.class))).thenReturn(reader);
    when(reader.city(any())).thenThrow(GeoIp2Exception.class);

    sut.loadDatabase();
    var actual = sut.extractRequestMetadata(mock(HttpServletRequest.class));
    assertThat(actual.isPresent(), is(false));
  }

  @Test
  void shouldReturnEmptyRequestMetadataIfIoExceptionOccurs() throws IOException, GeoIp2Exception {
    when(factory.create(any(Path.class))).thenReturn(reader);
    when(reader.city(any())).thenThrow(IOException.class);

    sut.loadDatabase();
    var actual = sut.extractRequestMetadata(mock(HttpServletRequest.class));
    assertThat(actual.isPresent(), is(false));
  }

  @Test
  void shouldReturnEmptyGeoMetadataWhenCityResponseHasNoData() throws IOException, GeoIp2Exception {
    when(factory.create(any(Path.class))).thenReturn(reader);
    sut.loadDatabase();

    var request = mock(HttpServletRequest.class);
    var cityResponse = mock(CityResponse.class);
    when(request.getHeader(anyString())).thenReturn("196.245.163.202");
    when(reader.city(any())).thenReturn(cityResponse);

    var requestMetadata = sut.extractRequestMetadata(request);
    assertThat(requestMetadata.isPresent(), is(true));
    assertThat(requestMetadata.get().getGeoMetadata().getCityName(), is(""));
    assertThat(requestMetadata.get().getGeoMetadata().getCountryIsoCode(), is(""));
    assertThat(requestMetadata.get().getGeoMetadata().getSubdivisionIsoCode(), is(""));
  }

  @Test
  void shouldAlertUnrecognisedDeviceIfNoRequestMetadataMatch() throws IOException, GeoIp2Exception {
    when(factory.create(any(Path.class))).thenReturn(reader);
    sut.loadDatabase();

    var request = mock(HttpServletRequest.class);
    var cityResponse = mock(CityResponse.class);
    when(request.getHeader(anyString())).thenReturn("196.245.163.202");
    when(reader.city(any())).thenReturn(cityResponse);
    when(securityLogService.findAllByAccountId(any(Account.class))).thenReturn(Collections.emptyList());

    var account = mock(Account.class);
    var result = sut.verifyRequest(account, request);

    assertThat(result.get().getIp(), is("196.245.163.202"));
    verify(securityLogService, times(1)).unrecognizedDevice(any(), any());
    verify(emailService, times(1)).sendUnrecognizedDevice(any(), any());
  }

  @Test
  void shouldNotMatchRequestMetadataWithNullSecurityLogRequestMetadata() throws IOException, GeoIp2Exception {
    when(factory.create(any(Path.class))).thenReturn(reader);
    sut.loadDatabase();

    var request = mock(HttpServletRequest.class);
    var cityResponse = mock(CityResponse.class);
    var securityLog = mock(SecurityLog.class);
    when(request.getHeader(anyString())).thenReturn("196.245.163.202");
    when(reader.city(any())).thenReturn(cityResponse);
    when(securityLogService.findAllByAccountId(any(Account.class))).thenReturn(List.of(securityLog));

    var account = mock(Account.class);
    var result = sut.verifyRequest(account, request);

    assertThat(result.get().getIp(), is("196.245.163.202"));
    verify(securityLogService, times(1)).unrecognizedDevice(any(), any());
    verify(emailService, times(1)).sendUnrecognizedDevice(any(), any());
  }

  @Test
  void shouldNotMatchRequestMetadataWithNullSecurityLogGeoMetadata() throws IOException, GeoIp2Exception {
    when(factory.create(any(Path.class))).thenReturn(reader);
    sut.loadDatabase();

    var request = mock(HttpServletRequest.class);
    var cityResponse = mock(CityResponse.class);
    var securityLog = mock(SecurityLog.class);
    var mockRequestMetadata = mock(RequestMetadata.class);
    when(mockRequestMetadata.getGeoMetadata()).thenReturn(null);
    when(securityLog.getRequestMetadata()).thenReturn(mockRequestMetadata);
    when(request.getHeader(anyString())).thenReturn("196.245.163.202");
    when(reader.city(any())).thenReturn(cityResponse);
    when(securityLogService.findAllByAccountId(any(Account.class))).thenReturn(List.of(securityLog));

    var account = mock(Account.class);
    var result = sut.verifyRequest(account, request);

    assertThat(result.get().getIp(), is("196.245.163.202"));
    verify(securityLogService, times(1)).unrecognizedDevice(any(), any());
    verify(emailService, times(1)).sendUnrecognizedDevice(any(), any());
  }

  @Test
  void shouldNotMatchRequestMetadataWithNullSecurityLogDeviceMetadata() throws IOException, GeoIp2Exception {
    when(factory.create(any(Path.class))).thenReturn(reader);
    sut.loadDatabase();

    var request = mock(HttpServletRequest.class);
    var cityResponse = mock(CityResponse.class);
    var securityLog = mock(SecurityLog.class);
    var mockRequestMetadata = mock(RequestMetadata.class);
    var geoMetadata = new GeoMetadata("", "", "");
    when(mockRequestMetadata.getGeoMetadata()).thenReturn(geoMetadata);
    when(securityLog.getRequestMetadata()).thenReturn(mockRequestMetadata);
    when(request.getHeader(anyString())).thenReturn("196.245.163.202");
    when(reader.city(any())).thenReturn(cityResponse);
    when(securityLogService.findAllByAccountId(any(Account.class))).thenReturn(List.of(securityLog));

    var account = mock(Account.class);
    var result = sut.verifyRequest(account, request);

    assertThat(result.get().getIp(), is("196.245.163.202"));
    verify(securityLogService, times(1)).unrecognizedDevice(any(), any());
    verify(emailService, times(1)).sendUnrecognizedDevice(any(), any());
  }
}