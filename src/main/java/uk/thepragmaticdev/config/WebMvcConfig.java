package uk.thepragmaticdev.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**");
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    var objectMapper = Jackson2ObjectMapperBuilder.json().modules(new JavaTimeModule(), new Jdk8Module()).build()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
  }
}