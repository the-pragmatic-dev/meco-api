package uk.thepragmaticdev.text.perspective.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.thepragmaticdev.text.perspective.AttributeScores;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseCommentResponse {

  private AttributeScores attributeScores;

  private List<String> languages;

  private List<String> detectedLanguages;
}
