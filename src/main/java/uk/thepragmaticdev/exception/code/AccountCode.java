package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum AccountCode implements ErrorCode {

  USERNAME_NOT_FOUND(HttpStatus.NOT_FOUND, "Username not found."),
  USERNAME_UNAVAILABLE(HttpStatus.CONFLICT, "Username is already in use.");

  private final String message;
  private final HttpStatus status;

  private AccountCode(HttpStatus status, String message) {
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
