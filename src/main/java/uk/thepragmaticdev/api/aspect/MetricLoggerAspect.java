package uk.thepragmaticdev.api.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import uk.thepragmaticdev.api.Model;

@Aspect
@Component
public class MetricLoggerAspect {

  private static final Logger logger = LoggerFactory.getLogger(MetricLoggerAspect.class);

  /**
   * TODO.
   * 
   * @param joinPoint TODO
   * @return TODO
   * @throws Throwable TODO
   */
  @Around("within(@org.springframework.web.bind.annotation.RestController *)")
  public Object logAfter(ProceedingJoinPoint joinPoint) throws Throwable {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    Object result = joinPoint.proceed();
    stopWatch.stop();

    JSONObject metric = createMetric(joinPoint, stopWatch);

    logger.info("{}", metric);

    return result;
  }

  private JSONObject createMetric(ProceedingJoinPoint joinPoint, StopWatch stopWatch) {
    JSONObject metric = new JSONObject();
    metric.put("class", joinPoint.getSignature().getDeclaringTypeName());
    metric.put("method", joinPoint.getSignature().getName());
    metric.put("millis", stopWatch.getTotalTimeMillis());
    metric.put("args", getArgs(joinPoint.getArgs()));
    metric.put("user", getUsername(joinPoint.getArgs()));
    return metric;
  }

  private JSONArray getArgs(Object[] args) {
    JSONArray objects = new JSONArray();
    for (Object object : args) {
      if (!(object instanceof UsernamePasswordAuthenticationToken) && !(object instanceof Pageable)) {
        if (object instanceof Model) {
          objects.put(serialiseModel((Model) object));
        } else {
          objects.put(object);
        }
      }
    }
    return objects;
  }

  private String serialiseModel(Model model) {
    try {
      return new ObjectMapper().writeValueAsString(model);
    } catch (JsonProcessingException e) {
      logger.info("Error serialising model");
      return "";
    }
  }

  private String getUsername(Object[] args) {
    String username = "unauthenticated";
    for (Object object : args) {
      if (object instanceof UsernamePasswordAuthenticationToken) {
        username = ((User) ((UsernamePasswordAuthenticationToken) object).getPrincipal()).getUsername();
      }
    }
    return username;
  }
}
