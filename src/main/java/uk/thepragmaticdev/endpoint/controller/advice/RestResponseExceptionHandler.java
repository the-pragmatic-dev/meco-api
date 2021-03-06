package uk.thepragmaticdev.endpoint.controller.advice;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.thepragmaticdev.exception.ApiError;
import uk.thepragmaticdev.exception.ApiException;

@Log4j2
@ControllerAdvice
public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {

  /**
   * Handles any api error exceptions thrown thoughout application.
   * 
   * @param ex      The api exception thrown
   * @param request The request metadata
   * @return The http response
   */
  @ExceptionHandler(ApiException.class)
  protected ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
    var responseBody = new ApiError(//
        ex.getErrorCode().getStatus(), //
        ex.getErrorCode().getMessage() //
    );
    log.warn(responseBody);
    return handleExceptionInternal(ex, responseBody, headers(), ex.getErrorCode().getStatus(), request);
  }

  /**
   * Handles any exception thrown by validation errors on bad requests.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(//
      MethodArgumentNotValidException ex, //
      HttpHeaders headers, //
      HttpStatus status, //
      WebRequest request) {
    var responseBody = new ApiError(HttpStatus.BAD_REQUEST, "Validation errors");
    responseBody.addValidationErrors(ex.getBindingResult().getFieldErrors());
    log.warn(responseBody);
    return handleExceptionInternal(ex, responseBody, headers(), HttpStatus.BAD_REQUEST, request);
  }

  /**
   * Handles any exception not caught by the above exception handlers.
   * 
   * @param ex      The general exception thrown
   * @param request The request metadata
   * @return The http response
   */
  @ExceptionHandler(Exception.class)
  protected ResponseEntity<Object> exception(Exception ex, WebRequest request) {
    log.error(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getStackTrace(ex)));
    return handleExceptionInternal(//
        ex, //
        new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, null), //
        headers(), HttpStatus.INTERNAL_SERVER_ERROR, //
        request//
    );
  }

  private HttpHeaders headers() {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
