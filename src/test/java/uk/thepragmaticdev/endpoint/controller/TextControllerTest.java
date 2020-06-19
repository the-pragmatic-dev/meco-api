package uk.thepragmaticdev.endpoint.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Mono;
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.exception.ApiException;
import uk.thepragmaticdev.exception.code.CriticalCode;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.security.key.ApiKeyAuthenticationToken;
import uk.thepragmaticdev.text.TextService;
import uk.thepragmaticdev.text.dto.request.TextRequest;
import uk.thepragmaticdev.text.perspective.dto.response.AnalyseCommentResponse;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TextControllerTest extends UnitData {

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockMvc mvc;

  @Mock
  private TextService textService;

  private ApiKeyAuthenticationToken token;

  private TextController sut;

  /**
   * Called before each test. Builds the system under test and mocks the mvc
   * endpoint.
   */
  @BeforeEach
  public void initEach() {
    sut = new TextController(textService);
    mvc = MockMvcBuilders.standaloneSetup(sut)//
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())//
        .build();
    token = mock(ApiKeyAuthenticationToken.class);
  }

  @Test
  void shouldMapToAnalyseCommentResponse() throws Exception {
    var analyseCommentResponse = analyseCommentResponse();
    var textRequest = new TextRequest("a very naughty sentence");

    when(token.getPrincipal()).thenReturn(mock(ApiKey.class));
    when(textService.analyse(anyString(), any(ApiKey.class))).thenReturn(Mono.just(analyseCommentResponse));

    var result = mvc.perform(//
        MockMvcRequestBuilders.post("/v1/text") //
            .principal(token) //
            .contentType(MediaType.APPLICATION_JSON) //
            .content(new Gson().toJson(textRequest)) //
            .accept(MediaType.APPLICATION_JSON) //
    ).andExpect(request().asyncStarted()).andReturn();

    result.getAsyncResult();

    var body = mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse().getContentAsString();

    var response = mapper.readValue(body, AnalyseCommentResponse.class);
    assertThat(response, is(analyseCommentResponse));
  }

  @Test
  void shouldThrowErrorIfAuthenticationFailed() throws Exception {
    var textRequest = new TextRequest("a very naughty sentence");

    when(token.getPrincipal()).thenReturn("bad principal");

    var ex = Assertions.assertThrows(ApiException.class, () -> {
      sut.analyse(token, textRequest);
    });
    assertThat(ex.getErrorCode(), is(CriticalCode.AUTHENTICATION_ERROR));
  }
}