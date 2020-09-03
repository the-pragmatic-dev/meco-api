package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

/**
 * An error code with a public constructor to hold errors from external REST
 * services.
 */
public class IntegrationCode implements ErrorCode {

  private final String message;
  private final HttpStatus status;

  public IntegrationCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  @Override
  public HttpStatus getStatus() {
    return status;
  }

  @Override
  public String getMessage() {
    return message;
  }
}