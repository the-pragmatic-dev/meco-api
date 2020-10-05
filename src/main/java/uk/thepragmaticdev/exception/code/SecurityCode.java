package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum SecurityCode implements ErrorCode {

  TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "You have exhausted your API Request Quota.");

  private final String message;
  private final HttpStatus status;

  private SecurityCode(HttpStatus status, String message) {
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
