package uk.thepragmaticdev.endpoint.controller.advice;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@ControllerAdvice
public class RestResponseExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(RestResponseExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  protected ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
    ApiError responseBody = new ApiError(//
        ex.getErrorCode().getStatus(), //
        ex.getErrorCode().getMessage() //
    );
    logger.warn("{}", responseBody);
    return handleExceptionInternal(ex, responseBody, headers(), ex.getErrorCode().getStatus(), request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(//
      MethodArgumentNotValidException ex, //
      HttpHeaders headers, //
      HttpStatus status, //
      WebRequest request) {
    ApiError responseBody = new ApiError(HttpStatus.BAD_REQUEST, "Validation errors");
    responseBody.addValidationErrors(ex.getBindingResult().getFieldErrors());
    logger.warn("{}", responseBody);
    return handleExceptionInternal(ex, responseBody, headers(), HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(Exception.class)
  protected ResponseEntity<Object> exception(Exception ex, WebRequest request) {
    logger.error("{}", new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionUtils.getStackTrace(ex)));
    return handleExceptionInternal(//
        ex, //
        new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, null), //
        headers(), HttpStatus.INTERNAL_SERVER_ERROR, //
        request//
    );
  }

  private HttpHeaders headers() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
