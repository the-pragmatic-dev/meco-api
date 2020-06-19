package uk.thepragmaticdev.text.perspective;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpanScore {

  private int begin;

  private int end;

  private Score score;
}
