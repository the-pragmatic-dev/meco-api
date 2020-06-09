package uk.thepragmaticdev.endpoint.aspect;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Log4j2
@Aspect
@Component
public class MetricLoggerAspect {

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
    log.info(createMetric(joinPoint, stopWatch));
    return result;
  }

  private Map<String, Object> createMetric(ProceedingJoinPoint joinPoint, StopWatch stopWatch) {
    Map<String, Object> metric = new HashMap<>();
    metric.put("class", joinPoint.getSignature().getDeclaringTypeName());
    metric.put("method", joinPoint.getSignature().getName());
    metric.put("millis", stopWatch.getTotalTimeMillis());
    metric.put("args", getArgs(joinPoint.getArgs()));
    metric.put("user", getUsername(joinPoint.getArgs()));
    return metric;
  }

  private Object getArgs(Object[] args) {
    for (var object : args) {
      if (!(object instanceof UsernamePasswordAuthenticationToken) && !(object instanceof Pageable)
          && !(object instanceof HttpServletRequest)) {
        return object;
      }
    }
    return null;
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
