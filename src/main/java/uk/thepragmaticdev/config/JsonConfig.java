package uk.thepragmaticdev.config;

import com.monitorjbl.json.JsonViewSupportFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class JsonConfig {

  @Autowired
  private MappingJackson2HttpMessageConverter converter;

  @Bean
  public JsonViewSupportFactoryBean views() {
    return new JsonViewSupportFactoryBean(converter.getObjectMapper());
  }
}
