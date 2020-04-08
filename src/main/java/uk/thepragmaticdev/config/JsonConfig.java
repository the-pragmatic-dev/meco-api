package uk.thepragmaticdev.config;

import com.monitorjbl.json.JsonViewSupportFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class JsonConfig {

  @Bean
  public JsonViewSupportFactoryBean views(MappingJackson2HttpMessageConverter converter) {
    return new JsonViewSupportFactoryBean(converter.getObjectMapper());
  }
}
