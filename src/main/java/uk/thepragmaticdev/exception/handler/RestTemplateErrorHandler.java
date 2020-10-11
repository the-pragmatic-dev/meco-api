package uk.thepragmaticdev.exception.handler;

import java.io.IOException;
import java.nio.charset.Charset;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResponseErrorHandler;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;

@Log4j2
@Component
public class RestTemplateErrorHandler implements ResponseErrorHandler {

  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    return response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError();
  }

  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    var body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
    log.error(body);
    throw new ApiException(CriticalCode.INTEGRATION_ERROR);
  }
}
