package uk.thepragmaticdev.account;

import org.springframework.security.core.GrantedAuthority;

/**
 * An account may contain multiple roles. These are added to the JWT auth claim.
 */
public enum Role implements GrantedAuthority {
  ROLE_ADMIN;

  public String getAuthority() {
    return name();
  }
}