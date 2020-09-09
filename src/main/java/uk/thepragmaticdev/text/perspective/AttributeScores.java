package uk.thepragmaticdev.text.perspective;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class AttributeScores {

  @JsonAlias({ "TOXICITY", "toxicity" })
  private AttributeScore toxicity;

  @JsonAlias({ "SEVERE_TOXICITY", "severe_toxicity" })
  private AttributeScore severeToxicity;

  @JsonAlias({ "IDENTITY_ATTACK", "identity_attack" })
  private AttributeScore identityAttack;

  @JsonAlias({ "INSULT", "insult" })
  private AttributeScore insult;

  @JsonAlias({ "PROFANITY", "profanity" })
  private AttributeScore profanity;

  @JsonAlias({ "THREAT", "threat" })
  private AttributeScore threat;
}
