package uk.thepragmaticdev.log;

import com.opencsv.bean.CsvBindByName;
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
  @CsvIgnore
  private Long id;

  @CsvBindByName
  private String action; // generated

  @CsvBindByName
  private OffsetDateTime createdDate; // generated
}