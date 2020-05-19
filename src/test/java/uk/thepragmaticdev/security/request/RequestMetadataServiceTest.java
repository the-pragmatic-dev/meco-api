package uk.thepragmaticdev.security.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import java.io.IOException;
import java.nio.file.Path;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.thepragmaticdev.email.EmailService;
import uk.thepragmaticdev.log.security.SecurityLogService;

@ExtendWith(MockitoExtension.class)
class RequestMetadataServiceTest {

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
    sut = new RequestMetadataService("database.mmdb", "url", "src/test/resources/geolite", emailService,
        securityLogService, factory);
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
}