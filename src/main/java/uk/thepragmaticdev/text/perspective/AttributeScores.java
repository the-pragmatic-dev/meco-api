package uk.thepragmaticdev.text.perspective;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class AttributeScores {

  @JsonProperty("TOXICITY")
  private AttributeScore toxicity;

  @JsonProperty("PROFANITY")
  private AttributeScore profanity;
}
