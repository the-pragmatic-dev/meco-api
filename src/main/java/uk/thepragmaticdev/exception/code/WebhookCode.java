package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum WebhookCode implements ErrorCode {

  DESERIALIIZATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occured while deserializing object."),
  SIGNATURE_VERIFICATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
      "An internal error occured while verifiying signature."),
  OBJECT_MISSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occured retrieving object.");

  private final String message;
  private final HttpStatus status;

  private WebhookCode(HttpStatus status, String message) {
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
