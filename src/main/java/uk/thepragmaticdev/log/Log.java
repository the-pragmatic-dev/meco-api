package uk.thepragmaticdev.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.opencsv.bean.CsvIgnore;
import java.time.OffsetDateTime;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class Log {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  @CsvIgnore
  private Long id;

  private String action; // generated

  private OffsetDateTime instant; // generated
}