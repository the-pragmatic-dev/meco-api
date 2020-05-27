package uk.thepragmaticdev.exception.code;

import org.springframework.http.HttpStatus;
import uk.thepragmaticdev.exception.ErrorCode;

public enum CriticalCode implements ErrorCode {

  CSV_WRITING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occured while processing the csv file."),
  GEOLITE_DOWNLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load geolite database."),
  PRINT_WRITER_IO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occured while processing the request."),
  TEMPLATE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to find email template.");

  private final String message;
  private final HttpStatus status;

  private CriticalCode(HttpStatus status, String message) {
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
