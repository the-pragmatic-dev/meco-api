package uk.thepragmaticdev.exception;

@SuppressWarnings("serial")
public class ApiException extends RuntimeException {

  private final transient ErrorCode errorCode;

  public ApiException(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  @Override
  public String getMessage() {
    return errorCode.getMessage();
  }
}
