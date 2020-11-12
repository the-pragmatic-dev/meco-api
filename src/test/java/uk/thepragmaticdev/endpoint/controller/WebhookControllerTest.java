package uk.thepragmaticdev.endpoint.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.thepragmaticdev.UnitData;
import uk.thepragmaticdev.webhook.WebhookService;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class WebhookControllerTest extends UnitData {

  @Autowired
  private MockMvc mvc;

  @Mock
  private WebhookService webhookService;

  private WebhookController sut;

  /**
   * Called before each test. Builds the system under test and mocks the mvc
   * endpoint.
   */
  @BeforeEach
  public void initEach() {
    sut = new WebhookController(webhookService);
    mvc = MockMvcBuilders.standaloneSetup(sut)//
        .build();
  }

  /**
   * Stripe webhooks require a 200 OK response to mark the event as received.
   * 
   * @throws Exception If MVC exception occurs
   */
  @Test
  void shouldReturn200Response() throws Exception {
    mvc.perform(//
        MockMvcRequestBuilders.post("/v1/webhooks/stripe")//
            .contentType(MediaType.APPLICATION_JSON)//
            .content(new Gson().toJson("{}")) //
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk());
  }
}