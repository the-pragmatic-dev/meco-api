package uk.thepragmaticdev.text.perspective;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class RequestedAttributes {

  @JsonProperty("TOXICITY")
  private Attribute toxicity;

  @JsonProperty("PROFANITY")
  private Attribute profanity;
}
