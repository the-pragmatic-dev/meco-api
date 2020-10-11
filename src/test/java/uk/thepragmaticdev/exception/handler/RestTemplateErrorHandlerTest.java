package uk.thepragmaticdev.exception.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;

@SpringBootTest
class RestTemplateErrorHandlerTest {

  private RestTemplateErrorHandler sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    sut = new RestTemplateErrorHandler();
  }

  @Test
  void shouldReturnFalseIfStatusCodeIsOk() throws IOException {
    var response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    assertThat(sut.hasError(response), is(false));
  }

  @Test
  void shouldReturnTrueIfStatusCodeIs4xx() throws IOException {
    var response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
    assertThat(sut.hasError(response), is(true));
  }

  @Test
  void shouldReturnTrueIfStatusCodeIs5xx() throws IOException {
    var response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(sut.hasError(response), is(true));
  }

  @Test
  void shouldThrowIntegrationApiExceptionWithGivenValues() throws IOException {
    var response = mock(ClientHttpResponse.class);
    when(response.getStatusCode()).thenReturn(HttpStatus.BAD_GATEWAY);
    when(response.getBody()).thenReturn(new ByteArrayInputStream(StandardCharsets.UTF_8.encode("message").array()));

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.handleError(response);
    });
    assertThat(ex.getErrorCode().getMessage(), is(CriticalCode.INTEGRATION_ERROR.getMessage()));
    assertThat(ex.getErrorCode().getStatus(), is(HttpStatus.SERVICE_UNAVAILABLE));
  }
}
