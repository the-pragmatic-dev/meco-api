package uk.thepragmaticdev.config.interceptor;

import com.hazelcast.core.HazelcastInstance;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.hazelcast.Hazelcast;
import java.time.Duration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.thepragmaticdev.exception.ApiError;
import uk.thepragmaticdev.exception.code.SecurityCode;

@Log4j2
public class RateLimitInterceptor implements HandlerInterceptor {

  private final ProxyManager<String> buckets;

  private final BucketConfiguration configuration;

  /**
   * Configure HazelcastProxyManager for IP rate limited interceptor map.
   * 
   * @param hazelcastInstance A Hazelcast instance to form a Hazelcast cluster.
   */
  public RateLimitInterceptor(HazelcastInstance hazelcastInstance) {
    this.buckets = Bucket4j.extension(Hazelcast.class)
        .proxyManagerForMap(hazelcastInstance.getMap("per-client-bucket-map"));
    this.configuration = Bucket4j.configurationBuilder()
        .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)))).build();
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    System.out.println(request.getRemoteAddr());
    Bucket bucket = buckets.getProxy(request.getRemoteAddr(), configuration);

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
      return true;
    }

    long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;
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
