package uk.thepragmaticdev.kms.scope;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class TextScope {

  @Column(name = "text_toxicity", columnDefinition = "boolean not null default false")
  private boolean toxicity;

  @Column(name = "text_severe_toxicity", columnDefinition = "boolean not null default false")
  private boolean severeToxicity;

  @Column(name = "text_identity_attack", columnDefinition = "boolean not null default false")
  private boolean identityAttack;

  @Column(name = "text_insult", columnDefinition = "boolean not null default false")
  private boolean insult;

  @Column(name = "text_profanity", columnDefinition = "boolean not null default false")
  private boolean profanity;

  @Column(name = "text_threat", columnDefinition = "boolean not null default false")
  private boolean threat;
}
