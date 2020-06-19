package uk.thepragmaticdev.text.perspective.dto.request;

import java.util.List;
import lombok.Data;
import uk.thepragmaticdev.text.perspective.Comment;
import uk.thepragmaticdev.text.perspective.RequestedAttributes;

@Data
public class AnalyseCommentRequest {

  private final Comment comment;

  private final List<String> languages;

  private final RequestedAttributes requestedAttributes;
}
