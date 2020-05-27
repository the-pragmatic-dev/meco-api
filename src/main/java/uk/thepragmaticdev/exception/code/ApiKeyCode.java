package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum ApiKeyCode implements ErrorCode {

  API_KEY_LIMIT(HttpStatus.FORBIDDEN, "You have requested too many API keys. Try deleting redundant keys."),
  API_KEY_NOT_FOUND(HttpStatus.NOT_FOUND, "API key not found.");

  private final String message;
  private final HttpStatus status;

  private ApiKeyCode(HttpStatus status, String message) {
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
