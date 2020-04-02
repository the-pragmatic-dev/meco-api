package uk.thepragmaticdev.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

@Data
@RequiredArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ApiError {

  private static final String PROTECTED_VALUE = "[PROTECTED]";

  private final HttpStatus status;
  private final String message;
  private List<ApiSubError> subErrors;

  /**
   * TODO.
   * 
   * @param fieldErrors TODO
   */
  public void addValidationErrors(List<FieldError> fieldErrors) {
    if (subErrors == null) {
      subErrors = new ArrayList<>();
    }

    fieldErrors.forEach((fieldError) -> {
      Object rejectedValue = maskRejectedPasswordValue(fieldError);
      this.subErrors.add(new ApiSubError(//
          fieldError.getObjectName(), //
          fieldError.getField(), //
          rejectedValue, //
          fieldError.getDefaultMessage() //
      ));
    });
  }

  private Object maskRejectedPasswordValue(FieldError fieldError) {
    if (fieldError.getField().equalsIgnoreCase("password")) {
      return PROTECTED_VALUE;
    }
    return fieldError.getRejectedValue();
  }

  @Override
  public String toString() {
    return new Gson().toJson(this);
  }

  @Data
  @AllArgsConstructor
  private class ApiSubError {

    private String object;
    private String field;
    private Object rejectedValue;
    private String message;

    @Override
    public String toString() {
      return new Gson().toJson(this);
    }
  }
}
