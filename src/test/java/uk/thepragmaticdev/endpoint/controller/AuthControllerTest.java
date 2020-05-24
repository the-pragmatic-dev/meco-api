package uk.thepragmaticdev.endpoint.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.auth.AuthService;
import uk.thepragmaticdev.auth.dto.request.AuthSigninRequest;
import uk.thepragmaticdev.auth.dto.request.AuthSignupRequest;
import uk.thepragmaticdev.auth.dto.response.AuthSigninResponse;
import uk.thepragmaticdev.auth.dto.response.AuthSignupResponse;
import uk.thepragmaticdev.security.token.TokenPair;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends UnitData {

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockMvc mvc;

  @Mock
  private AuthService authService;

  private AuthController sut;

  /**
   * Called before each test. Builds the system under test and mocks the mvc
   * endpoint.
   */
  @BeforeEach
  public void initEach() {
    sut = new AuthController(authService, new ModelMapper());
    mvc = MockMvcBuilders.standaloneSetup(sut)//
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())//
        .build();
  }

  @Test
  void shouldMapToAuthSigninResponse() throws Exception {
    var tokenPair = new TokenPair("access", UUID.randomUUID());
    var authSigninRequest = new AuthSigninRequest("username@email.com", "password");

    when(authService.signin(anyString(), anyString(), any(HttpServletRequest.class))).thenReturn(tokenPair);

    var body = mvc.perform(//
        MockMvcRequestBuilders.post("/auth/signin")//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson(authSigninRequest)) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, AuthSigninResponse.class);

    assertThat(response.getAccessToken(), is(tokenPair.getAccessToken()));
    assertThat(response.getRefreshToken(), is(tokenPair.getRefreshToken()));
  }

  @Test
  void shouldMapToAuthSignupResponse() throws Exception {
    var tokenPair = new TokenPair("access", UUID.randomUUID());
    var authSignupRequest = new AuthSignupRequest("username@email.com", "password");

    when(authService.signup(anyString(), anyString(), any(HttpServletRequest.class))).thenReturn(tokenPair);

    var body = mvc.perform(//
        MockMvcRequestBuilders.post("/auth/signup")//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson(authSignupRequest)) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, AuthSignupResponse.class);

    assertThat(response.getAccessToken(), is(tokenPair.getAccessToken()));
    assertThat(response.getRefreshToken(), is(tokenPair.getRefreshToken()));
  }

}