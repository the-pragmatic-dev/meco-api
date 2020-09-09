package uk.thepragmaticdev.kms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextScopeRequest {

  private boolean toxicity;

  private boolean severeToxicity;

  private boolean identityAttack;

  private boolean insult;

  private boolean profanity;

  private boolean threat;
}
