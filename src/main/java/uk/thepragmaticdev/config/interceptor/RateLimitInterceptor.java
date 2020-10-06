package uk.thepragmaticdev.config.interceptor;

import com.hazelcast.core.HazelcastInstance;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.hazelcast.Hazelcast;
import java.time.Duration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.thepragmaticdev.exception.ApiError;
import uk.thepragmaticdev.exception.code.SecurityCode;

@Log4j2
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private final ProxyManager<String> buckets;

  private final BucketConfiguration configuration;

  /**
   * Configure HazelcastProxyManager for IP rate limited interceptor map.
   * 
   * @param properties        The bucket properties.
   * @param hazelcastInstance A Hazelcast instance to form a Hazelcast cluster.
   */
  public RateLimitInterceptor(BucketProperties properties, HazelcastInstance hazelcastInstance) {
    log.info(properties);
    this.buckets = Bucket4j.extension(Hazelcast.class)
        .proxyManagerForMap(hazelcastInstance.getMap(properties.getName()));
    this.configuration = Bucket4j.configurationBuilder().addLimit(//
        Bandwidth.classic(properties.getCapacity(), //
            Refill.intervally(properties.getTokens(), //
                Duration.ofMinutes(properties.getMinutes()))))
        .build();
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    var bucket = buckets.getProxy(request.getRemoteAddr(), configuration);

    var probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      return true;
    }

    var waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
    response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));
    var responseBody = new ApiError(//
        SecurityCode.TOO_MANY_REQUESTS.getStatus(), //
        SecurityCode.TOO_MANY_REQUESTS.getMessage() //
    );
    log.warn("{}", responseBody);
    response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(SecurityCode.TOO_MANY_REQUESTS.getStatus().value());
    response.getWriter().write(responseBody.toString());
    return false;
  }
}
