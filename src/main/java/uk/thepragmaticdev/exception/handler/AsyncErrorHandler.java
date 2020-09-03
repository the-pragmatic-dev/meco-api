package uk.thepragmaticdev.exception.handler;

import java.lang.reflect.Method;
import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import uk.thepragmaticdev.exception.ApiError;
import uk.thepragmaticdev.exception.ApiException;

@Log4j2
public class AsyncErrorHandler implements AsyncUncaughtExceptionHandler {

  @Override
  public void handleUncaughtException(Throwable ex, Method method, Object... params) {
    if (ex instanceof ApiException) {
      var apiException = (ApiException) ex;
      var responseBody = new ApiError(//
          apiException.getErrorCode().getStatus(), //
          apiException.getErrorCode().getMessage() //
      );
      log.warn(responseBody);
    } else {
      log.error(ex);
    }
  }
}