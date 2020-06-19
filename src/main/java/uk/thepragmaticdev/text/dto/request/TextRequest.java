package uk.thepragmaticdev.text.dto.request;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextRequest {

  @NotBlank(message = "Text cannot be blank")
  String text;
}