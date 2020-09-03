package uk.thepragmaticdev.exception.handler;

import java.io.IOException;
import java.nio.charset.Charset;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.IntegrationCode;

@Component
public class RestTemplateErrorHandler implements ResponseErrorHandler {

  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    return response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError();
  }

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    var body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
    throw new ApiException(new IntegrationCode(response.getStatusCode(), body));
  }
}
