package uk.thepragmaticdev.security;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

@Service
public class RequestService {

  /**
   * Find the IP address of the clients request.
   * 
   * @param request The request information for http servlets
   * @return The clients IP
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