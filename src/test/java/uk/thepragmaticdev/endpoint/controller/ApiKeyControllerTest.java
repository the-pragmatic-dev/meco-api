package uk.thepragmaticdev.endpoint.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.opencsv.bean.StatefulBeanToCsv;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.kms.AccessPolicy;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.kms.ApiKeyService;
import uk.thepragmaticdev.kms.Scope;
import uk.thepragmaticdev.kms.dto.request.AccessPolicyRequest;
import uk.thepragmaticdev.kms.dto.request.ApiKeyCreateRequest;
import uk.thepragmaticdev.kms.dto.request.ApiKeyUpdateRequest;
import uk.thepragmaticdev.kms.dto.request.ScopeRequest;
import uk.thepragmaticdev.kms.dto.response.AccessPolicyResponse;
import uk.thepragmaticdev.kms.dto.response.ApiKeyCreateResponse;
import uk.thepragmaticdev.kms.dto.response.ApiKeyResponse;
import uk.thepragmaticdev.kms.dto.response.ScopeResponse;
import uk.thepragmaticdev.log.dto.ApiKeyLogResponse;
import uk.thepragmaticdev.log.key.ApiKeyLog;
import uk.thepragmaticdev.security.request.RequestMetadata;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ApiKeyControllerTest extends UnitData {

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockMvc mvc;

  @Mock
  private ApiKeyService apiKeyService;

  @Mock
  private StatefulBeanToCsv<ApiKeyLog> apiKeyLogWriter;

  private Principal principal;

  private ApiKeyController sut;

  /**
   * Called before each test. Builds the system under test, mocks and mvc endpoint
   * and creates a test principal for authentication.
   */
  @BeforeEach
  public void initEach() {
    sut = new ApiKeyController(apiKeyService, new ModelMapper(), apiKeyLogWriter);
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
  void shouldMapToListOfApiKeyResponse() throws Exception {
    var expected = apiKey();
    when(apiKeyService.findAll(anyString())).thenReturn(List.of(expected));

    var body = mvc.perform(//
        MockMvcRequestBuilders.get("/api-keys")//
            .principal(principal)//
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, new TypeReference<List<ApiKeyResponse>>() {
    });

    assertThat(response.size(), is(1));
    response.forEach(actual -> assertValidApiKeyResponse(actual, expected));
  }

  @Test
  void shouldMapToApiKeyCreateResponse() throws Exception {
    var key = apiKey();
    var request = new ApiKeyCreateRequest(key.getName(), key.getEnabled(), scopeRequest(),
        List.of(accessPolicyRequest()));
    when(apiKeyService.create(anyString(), any(ApiKey.class))).thenReturn((key));

    var body = mvc.perform(//
        MockMvcRequestBuilders.post("/api-keys")//
            .principal(principal)//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson(request)) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, ApiKeyCreateResponse.class);
    assertValidApiKeyCreateResponse(response, key);
  }

  @Test
  void shouldMapToApiKeyResponse() throws Exception {
    var key = apiKey();
    var request = new ApiKeyUpdateRequest(key.getName(), key.getEnabled(), scopeRequest(),
        List.of(accessPolicyRequest()));
    when(apiKeyService.update(anyString(), anyLong(), any(ApiKey.class))).thenReturn((key));

    var body = mvc.perform(//
        MockMvcRequestBuilders.put("/api-keys/1")//
            .principal(principal)//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson(request)) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    var response = mapper.readValue(body, ApiKeyResponse.class);
    assertValidApiKeyResponse(response, key);
  }

  @Test
  void shouldMapToPageOfApiKeyLogResponses() throws Exception {
    var logs = apiKeyLogs();
    when(apiKeyService.log(any(Pageable.class), anyString(), anyLong())).thenReturn(logs);

    var body = mvc.perform(//
        MockMvcRequestBuilders.get("/api-keys/1/logs")//
            .principal(principal)//
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    var actual = pageToList(body, ApiKeyLogResponse.class);
    var expected = logs.getContent();
    for (int i = 0; i < actual.size(); i++) {
      assertThat(actual.get(i).getAction(), is(expected.get(i).getAction()));
      assertThat(actual.get(i).getRequestMetadata(), is(expected.get(i).getRequestMetadata()));
      assertThat(actual.get(i).getCreatedDate(), is(expected.get(i).getCreatedDate()));
    }
  }

  private void assertValidApiKeyResponse(ApiKeyResponse actual, ApiKey expected) {
    assertThat(actual.getId(), is(expected.getId()));
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getPrefix(), is(expected.getPrefix()));
    assertThat(actual.getCreatedDate(), is(expected.getCreatedDate()));
    assertThat(actual.getLastUsedDate(), is(expected.getLastUsedDate()));
    assertThat(actual.getModifiedDate(), is(expected.getModifiedDate()));
    assertThat(actual.getEnabled(), is(expected.getEnabled()));
    assertValidScope(actual.getScope(), expected.getScope());
    for (int i = 0; i < actual.getAccessPolicies().size(); i++) {
      assertValidAccessPolicy(actual.getAccessPolicies().get(i), expected.getAccessPolicies().get(i));
    }
  }

  private void assertValidApiKeyCreateResponse(ApiKeyCreateResponse actual, ApiKey expected) {
    assertThat(actual.getId(), is(expected.getId()));
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getPrefix(), is(expected.getPrefix()));
    assertThat(actual.getKey(), is(expected.getKey()));
    assertThat(actual.getCreatedDate(), is(expected.getCreatedDate()));
    assertThat(actual.getLastUsedDate(), is(expected.getLastUsedDate()));
    assertThat(actual.getModifiedDate(), is(expected.getModifiedDate()));
    assertThat(actual.getEnabled(), is(expected.getEnabled()));
    assertValidScope(actual.getScope(), expected.getScope());
    for (int i = 0; i < actual.getAccessPolicies().size(); i++) {
      assertValidAccessPolicy(actual.getAccessPolicies().get(i), expected.getAccessPolicies().get(i));
    }
  }

  private void assertValidScope(ScopeResponse actual, Scope expected) {
    assertThat(actual.getImage(), is(expected.getImage()));
    assertThat(actual.getGif(), is(expected.getGif()));
    assertThat(actual.getText(), is(expected.getText()));
    assertThat(actual.getVideo(), is(expected.getVideo()));
  }

  private void assertValidAccessPolicy(AccessPolicyResponse actual, AccessPolicy expected) {
    assertThat(actual.getName(), is(expected.getName()));
    assertThat(actual.getRange(), is(expected.getRange()));
  }

  private ApiKey apiKey() {
    var key = new ApiKey();
    key.setId(1L);
    key.setName("name");
    key.setPrefix("prefix");
    key.setKey("key");
    key.setCreatedDate(OffsetDateTime.now(ZoneOffset.UTC));
    key.setLastUsedDate(OffsetDateTime.now(ZoneOffset.UTC));
    key.setModifiedDate(OffsetDateTime.now(ZoneOffset.UTC));
    key.setEnabled(true);
    key.setScope(scope(1, key));
    key.setAccessPolicies(List.of(accessPolicy(1, key)));
    return key;
  }

  private Scope scope(long id, ApiKey key) {
    return new Scope(id, true, true, true, true, key);
  }

  private AccessPolicy accessPolicy(long id, ApiKey key) {
    return new AccessPolicy(id, "name", "range", key);
  }

  private ScopeRequest scopeRequest() {
    return new ScopeRequest(true, true, true, true);
  }

  private AccessPolicyRequest accessPolicyRequest() {
    return new AccessPolicyRequest("name", "127.0.0.1/32");
  }

  private Page<ApiKeyLog> apiKeyLogs() {
    var apiKeyLogs = List.of(//
        new ApiKeyLog(1L, apiKey(), "action1", metadata(), OffsetDateTime.now(ZoneOffset.UTC)), //
        new ApiKeyLog(1L, apiKey(), "action1", metadata(), OffsetDateTime.now(ZoneOffset.UTC)));
    return new PageImpl<ApiKeyLog>(apiKeyLogs, PageRequest.of(1, 1), apiKeyLogs.size());
  }

  private RequestMetadata metadata() {
    return new RequestMetadata();
  }
}