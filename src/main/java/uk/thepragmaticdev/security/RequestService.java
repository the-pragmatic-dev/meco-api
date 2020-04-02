package uk.thepragmaticdev.security;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestService {

  /**
   * TODO.
   * 
   * @param request TODO
   * @return
   */
  public String getClientIp(HttpServletRequest request) {
    String remoteAddr = "";
    if (request != null) {
      remoteAddr = request.getHeader("X-FORWARDED-FOR");
      if (remoteAddr == null || "".equals(remoteAddr)) {
        remoteAddr = request.getRemoteAddr();
      }
    }
    return remoteAddr;
  }
}