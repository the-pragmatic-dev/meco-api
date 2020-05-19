package uk.thepragmaticdev.config;

import com.opencsv.ICSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.annotation.RequestScope;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.key.ApiKeyLog;
import uk.thepragmaticdev.log.security.SecurityLog;

@Configuration
public class CsvWriterConfig {

  private static final String ATTACHEMENT_PREFIX = "attachment; filename=\"";

  /**
   * A stateful bean to csv writer for billing logs.
   * 
   * @param response The servlet response
   * @return A csv writer
   * @throws IOException If unable to get writer
   */
  @Bean
  @RequestScope
  public StatefulBeanToCsv<BillingLog> billingLogCsvWriter(HttpServletResponse response) throws IOException {
    updateResponse(response);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ATTACHEMENT_PREFIX + "billing.csv" + "\"");
    return new StatefulBeanToCsvBuilder<BillingLog>(response.getWriter()).withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
        .withSeparator(ICSVWriter.DEFAULT_SEPARATOR).withOrderedResults(true).build();
  }

  /**
   * A stateful bean to csv writer for api key logs.
   * 
   * @param response The servlet response
   * @return A csv writer
   * @throws IOException If unable to get writer
   */
  @Bean
  @RequestScope
  public StatefulBeanToCsv<ApiKeyLog> apiKeyLogCsvWriter(HttpServletResponse response) throws IOException {
    updateResponse(response);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ATTACHEMENT_PREFIX + "api-key.csv" + "\"");
    return new StatefulBeanToCsvBuilder<ApiKeyLog>(response.getWriter()).withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
        .withSeparator(ICSVWriter.DEFAULT_SEPARATOR).withOrderedResults(true).build();
  }

  /**
   * A stateful bean to csv writer for security logs.
   * 
   * @param response The servlet response
   * @return A csv writer
   * @throws IOException If unable to get writer
   */
  @Bean
  @RequestScope
  public StatefulBeanToCsv<SecurityLog> securityLogCsvWriter(HttpServletResponse response) throws IOException {
    updateResponse(response);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ATTACHEMENT_PREFIX + "security.csv" + "\"");
    return new StatefulBeanToCsvBuilder<SecurityLog>(response.getWriter()).withQuotechar(ICSVWriter.NO_QUOTE_CHARACTER)
        .withSeparator(ICSVWriter.DEFAULT_SEPARATOR).withOrderedResults(true).build();
  }

  private void updateResponse(HttpServletResponse response) {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
  }

}