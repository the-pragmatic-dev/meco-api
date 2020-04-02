package uk.thepragmaticdev.exception;

@SuppressWarnings("serial")
public class ApiException extends RuntimeException {

  private final ErrorCode errorCode;

  public ApiException(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
