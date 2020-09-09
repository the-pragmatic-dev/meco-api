package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum TextCode implements ErrorCode {

  TEXT_DISABLED(HttpStatus.FORBIDDEN, "API key does not have any text scopes enabled.");

  private final String message;
  private final HttpStatus status;

  private TextCode(HttpStatus status, String message) {
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
