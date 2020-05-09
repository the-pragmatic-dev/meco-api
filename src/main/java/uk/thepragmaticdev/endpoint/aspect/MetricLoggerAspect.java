package uk.thepragmaticdev.endpoint.aspect;

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
import uk.thepragmaticdev.endpoint.Model;

@Aspect
@Component
public class MetricLoggerAspect {

  private static final Logger LOG = LoggerFactory.getLogger(MetricLoggerAspect.class);

  /**
   * Advice that surrounds the join point of a methods invocation. This performs
   * custom logging of controller metrics between the endpoints invocation.
   * 
   * @param joinPoint The point during the execution of a method
   * @return The value of join point
   * @throws Throwable If the invoked proceed throws anything
   * 
   */
  @Around("within(@org.springframework.web.bind.annotation.RestController *)")
  public Object logAfter(ProceedingJoinPoint joinPoint) throws Throwable {
    var stopWatch = new StopWatch();
    stopWatch.start();
    var result = joinPoint.proceed();
    stopWatch.stop();
    var metric = createMetric(joinPoint, stopWatch);
    LOG.info("{}", metric);
    return result;
  }

  private JSONObject createMetric(ProceedingJoinPoint joinPoint, StopWatch stopWatch) {
    var metric = new JSONObject();
    metric.put("class", joinPoint.getSignature().getDeclaringTypeName());
    metric.put("method", joinPoint.getSignature().getName());
    metric.put("millis", stopWatch.getTotalTimeMillis());
    metric.put("args", getArgs(joinPoint.getArgs()));
    metric.put("user", getUsername(joinPoint.getArgs()));
    return metric;
  }

  private JSONArray getArgs(Object[] args) {
    var objects = new JSONArray();
    for (var object : args) {
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
    } catch (JsonProcessingException ex) {
      LOG.info("Error serialising model");
      return "";
    }
  }

  private String getUsername(Object[] args) {
    var username = "unauthenticated";
    for (var object : args) {
      if (object instanceof UsernamePasswordAuthenticationToken) {
        username = ((User) ((UsernamePasswordAuthenticationToken) object).getPrincipal()).getUsername();
      }
    }
    return username;
  }
}
