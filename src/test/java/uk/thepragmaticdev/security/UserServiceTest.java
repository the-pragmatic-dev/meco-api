package uk.thepragmaticdev.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountRepository;
import uk.thepragmaticdev.account.Role;

@SpringBootTest
class UserServiceTest {

  @Mock
  private AccountRepository accountRepository;

  private UserService sut;

  /**
   * Called before each test, builds the system under test.
   */
  @BeforeEach
  public void initEach() {
    sut = new UserService(accountRepository);
  }

  @Test
  void shouldLoadUserByUsername() {
    var username = "username";
    var password = "password";
    var account = new Account();
    account.setPassword(password);
    account.setRoles(List.of(Role.ROLE_ADMIN));

    when(accountRepository.findByUsername(username)).thenReturn(Optional.of(account));

    var ret = sut.loadUserByUsername(username);
    assertThat(ret.getPassword(), is(password));
    assertThat(ret.getAuthorities(), hasSize(1));
    assertThat(ret.getAuthorities(), contains(Role.ROLE_ADMIN));
    assertThat(ret.isAccountNonExpired(), is(true));
    assertThat(ret.isAccountNonLocked(), is(true));
    assertThat(ret.isCredentialsNonExpired(), is(true));
    assertThat(ret.isEnabled(), is(true));
  }

  @Test()
  void shouldThrowExceptionWhenUserNotFound() {
    var username = "username";
    when(accountRepository.findByUsername(anyString())).thenReturn(Optional.empty());

    UsernameNotFoundException ex = Assertions.assertThrows(UsernameNotFoundException.class, () -> {
      sut.loadUserByUsername(username);
    });
    assertThat(ex.getMessage(), is(username));
  }
}