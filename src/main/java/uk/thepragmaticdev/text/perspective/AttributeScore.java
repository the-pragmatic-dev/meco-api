package uk.thepragmaticdev.text.perspective;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributeScore {

  private List<SpanScore> spanScores;

  private Score summaryScore;
}
