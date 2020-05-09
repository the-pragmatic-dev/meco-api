package uk.thepragmaticdev.endpoint.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.account.AccountService;
import uk.thepragmaticdev.account.dto.request.AccountSigninRequest;
import uk.thepragmaticdev.account.dto.request.AccountSignupRequest;
import uk.thepragmaticdev.account.dto.request.AccountUpdateRequest;
import uk.thepragmaticdev.account.dto.response.AccountMeResponse;
import uk.thepragmaticdev.account.dto.response.AccountSigninResponse;
import uk.thepragmaticdev.account.dto.response.AccountSignupResponse;
import uk.thepragmaticdev.account.dto.response.AccountUpdateResponse;
import uk.thepragmaticdev.log.billing.BillingLog;
import uk.thepragmaticdev.log.dto.BillingLogResponse;
import uk.thepragmaticdev.log.dto.SecurityLogResponse;
import uk.thepragmaticdev.log.security.SecurityLog;
import uk.thepragmaticdev.security.request.RequestMetadata;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
public class AccountControllerTest {

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockMvc mvc;

  @Mock
  private AccountService accountService;

  private Principal principal;

  private AccountController sut;

  @BeforeEach
  public void initEach() {
    sut = new AccountController(accountService, new ModelMapper());
    mvc = MockMvcBuilders.standaloneSetup(sut)//
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())//
        .build();
    principal = new Principal() {
      @Override
      public String getName() {
        return "principal";
      }
    };
  }

  @Test
  public void shouldMapToAccountSigninResponse() throws Exception {
    var token = "token";
    var accountSigninRequest = new AccountSigninRequest("username@email.com", "password");

    when(accountService.signin(anyString(), anyString())).thenReturn(token);

    var body = mvc.perform(//
        MockMvcRequestBuilders.post("/accounts/signin")//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson(accountSigninRequest)) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, AccountSigninResponse.class);

    assertThat(response.getToken(), is(token));
  }

  @Test
  public void shouldMapToAccountSignupResponse() throws Exception {
    var token = "token";
    var accountSignupRequest = new AccountSignupRequest("username@email.com", "password");

    when(accountService.signup(anyString(), anyString())).thenReturn(token);

    var body = mvc.perform(//
        MockMvcRequestBuilders.post("/accounts/signup")//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson(accountSignupRequest)) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, AccountSignupResponse.class);

    assertThat(response.getToken(), is(token));
  }

  @Test
  public void shouldMapToAccountMeResponse() throws Exception {
    var account = account();
    when(accountService.findAuthenticatedAccount(anyString())).thenReturn(account);

    var body = mvc.perform(//
        MockMvcRequestBuilders.get("/accounts/me")//
            .principal(principal)//
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, AccountMeResponse.class);

    assertThat(response.getUsername(), is(account.getUsername()));
    assertThat(response.getFullName(), is(account.getFullName()));
    assertThat(response.getEmailSubscriptionEnabled(), is(account.getEmailSubscriptionEnabled()));
    assertThat(response.getBillingAlertEnabled(), is(account.getBillingAlertEnabled()));
    assertThat(response.getCreatedDate(), is(account.getCreatedDate()));
  }

  @Test
  public void shouldMapToAccountUpdateResponse() throws Exception {
    var account = account();
    var accountUpdateRequest = new AccountUpdateRequest(//
        account.getFullName(), //
        account.getEmailSubscriptionEnabled(), //
        account.getBillingAlertEnabled());
    when(accountService.update(anyString(), anyString(), anyBoolean(), anyBoolean())).thenReturn(account);

    var body = mvc.perform(//
        MockMvcRequestBuilders.put("/accounts/me")//
            .principal(principal)//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson(accountUpdateRequest)) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, AccountUpdateResponse.class);

    assertThat(response.getUsername(), is(account.getUsername()));
    assertThat(response.getFullName(), is(account.getFullName()));
    assertThat(response.getEmailSubscriptionEnabled(), is(account.getEmailSubscriptionEnabled()));
    assertThat(response.getBillingAlertEnabled(), is(account.getBillingAlertEnabled()));
    assertThat(response.getCreatedDate(), is(account.getCreatedDate()));
  }

  @Test
  public void shouldMapToPageOfBillingLogResponses() throws Exception {
    var logs = billingLogs();
    when(accountService.billingLogs(any(Pageable.class), anyString())).thenReturn(logs);

    var body = mvc.perform(//
        MockMvcRequestBuilders.get("/accounts/me/billing/logs")//
            .principal(principal)//
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    var actual = pageToList(body, BillingLogResponse.class);
    var expected = logs.getContent();
    for (int i = 0; i < actual.size(); i++) {
      assertThat(actual.get(i).getAction(), is(expected.get(i).getAction()));
      assertThat(actual.get(i).getAmount(), is(expected.get(i).getAmount()));
      assertThat(actual.get(i).getCreatedDate(), is(expected.get(i).getCreatedDate()));
    }
  }

  @Test
  public void shouldMapToPageOfSecurityLogResponses() throws Exception {
    var logs = securityLogs();
    when(accountService.securityLogs(any(Pageable.class), anyString())).thenReturn(logs);

    var body = mvc.perform(//
        MockMvcRequestBuilders.get("/accounts/me/security/logs")//
            .principal(principal)//
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    var actual = pageToList(body, SecurityLogResponse.class);
    var expected = logs.getContent();
    for (int i = 0; i < actual.size(); i++) {
      assertThat(actual.get(i).getAction(), is(expected.get(i).getAction()));
      assertThat(actual.get(i).getRequestMetadata(), is(expected.get(i).getRequestMetadata()));
      assertThat(actual.get(i).getCreatedDate(), is(expected.get(i).getCreatedDate()));
    }
  }

  private <T> List<T> pageToList(String body, Class<T> type) throws Exception {
    var object = new JsonParser().parse(body).getAsJsonObject();
    var elements = object.getAsJsonArray("content");
    var logs = new ArrayList<T>();
    for (JsonElement element : elements) {
      T log = mapper.readValue(element.toString(), type);
      logs.add(log);
    }
    return logs;
  }

  private Account account() {
    var account = new Account();
    account.setUsername("username");
    account.setFullName("fullName");
    account.setEmailSubscriptionEnabled(true);
    account.setBillingAlertEnabled(false);
    account.setCreatedDate(OffsetDateTime.now(ZoneOffset.UTC));
    return account;
  }

  private Page<BillingLog> billingLogs() {
    var billingLogs = List.of(//
        new BillingLog(1L, account(), "action1", "amount1", OffsetDateTime.now(ZoneOffset.UTC)), //
        new BillingLog(2L, account(), "action2", "amount2", OffsetDateTime.now(ZoneOffset.UTC)) //
    );
    return new PageImpl<BillingLog>(billingLogs, PageRequest.of(1, 1), billingLogs.size());
  }

  private Page<SecurityLog> securityLogs() {
    var securityLogs = List.of(//
        new SecurityLog(1L, account(), "action1", metadata(), OffsetDateTime.now(ZoneOffset.UTC)), //
        new SecurityLog(1L, account(), "action1", metadata(), OffsetDateTime.now(ZoneOffset.UTC)));
    return new PageImpl<SecurityLog>(securityLogs, PageRequest.of(1, 1), securityLogs.size());
  }

  private RequestMetadata metadata() {
    return new RequestMetadata();
  }
}