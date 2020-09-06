package uk.thepragmaticdev.security.key;

import lombok.EqualsAndHashCode;
import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * An {@link org.springframework.security.core.Authentication} implementation
 * that is designed for simple presentation of an API key. The
 * <code>principal</code> should be set to an
 * {@link uk.thepragmaticdev.kms.ApiKey}.
 */
@SuppressWarnings("serial")
@EqualsAndHashCode(callSuper = true)
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

  private final transient Object principal;

  private transient Object credentials;

  /**
   * Produce a trusted (i.e. {@link #isAuthenticated()} = <code>true</code>)
   * authentication token containing an authenticated
   * {@link uk.thepragmaticdev.kms.ApiKey}. No authorites are granted.
   * 
   * @param principal An authenticated api key
   */
  public ApiKeyAuthenticationToken(Object principal) {
    super(null);
    this.principal = principal;
    this.credentials = "";
    super.setAuthenticated(true); // must use super, as we override
  }

  public Object getCredentials() {
    return this.credentials;
  }

  public Object getPrincipal() {
    return this.principal;
  }

  /**
   * A token cannot manually be set to trusted. Use the constructor to create a
   * trusted token.
   */
  @Override
  public void setAuthenticated(boolean isAuthenticated) {
    if (isAuthenticated) {
      throw new IllegalArgumentException("Cannot set this token to trusted - use constructor instead.");
    }
    super.setAuthenticated(false);
  }

  @Override
  public void eraseCredentials() {
    super.eraseCredentials();
    credentials = null;
  }
}
