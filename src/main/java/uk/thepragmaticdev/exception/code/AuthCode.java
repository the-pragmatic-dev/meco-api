package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum AuthCode implements ErrorCode {

  ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Expired or invalid access token."),
  API_KEY_INVALID(HttpStatus.UNAUTHORIZED, "Invalid API key."),
  CREDENTIALS_INVALID(HttpStatus.UNAUTHORIZED, "Your credentials are missing from the request, or aren't correct."),
  PASSWORD_RESET_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "Password reset token not found."),
  PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Password reset token expired."),
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "Refresh token not found."),
  REFRESH_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "Refresh token expired."),
  REQUEST_METADATA_INVALID(HttpStatus.BAD_REQUEST, "Unable to verify ip or user agent information.");

  private final String message;
  private final HttpStatus status;

  private AuthCode(HttpStatus status, String message) {
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
