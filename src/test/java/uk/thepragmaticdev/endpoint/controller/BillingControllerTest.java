package uk.thepragmaticdev.endpoint.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Price;
import com.stripe.model.Price.Recurring;
import com.stripe.model.Price.Tier;
import com.stripe.model.PriceCollection;
import java.util.List;
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
import uk.thepragmaticdev.billing.BillingService;
import uk.thepragmaticdev.billing.dto.response.BillingPriceRecurringResponse;
import uk.thepragmaticdev.billing.dto.response.BillingPriceResponse;
import uk.thepragmaticdev.billing.dto.response.BillingPriceTierResponse;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class BillingControllerTest extends UnitData {

  @Autowired
  private ObjectMapper mapper;

  @Autowired
  private MockMvc mvc;

  @Mock
  private BillingService billingService;

  private BillingController sut;

  /**
   * Called before each test. Builds the system under test, mocks and mvc
   * endpoint.
   */
  @BeforeEach
  public void initEach() {
    sut = new BillingController(billingService, new ModelMapper());
    mvc = MockMvcBuilders.standaloneSetup(sut)//
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())//
        .build();
  }

  @Test
  void shouldMapToListOfBillingPriceResponse() throws Exception {
    var expected = List.of(price());
    var priceCollection = mock(PriceCollection.class);
    when(priceCollection.getData()).thenReturn(expected);
    when(billingService.findAllPrices()).thenReturn(priceCollection);

    var body = mvc.perform(//
        MockMvcRequestBuilders.get("/billing/prices")//
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, new TypeReference<List<BillingPriceResponse>>() {
    });

    assertThat(response, hasSize(1));
    assertValidBillingPriceResponse(response.get(0), expected.get(0));
  }

  private void assertValidBillingPriceResponse(BillingPriceResponse actual, Price expected) {
    assertThat(actual.getId(), is(expected.getId()));
    assertThat(actual.getCurrency(), is(expected.getCurrency()));
    assertThat(actual.getNickname(), is(expected.getNickname()));
    assertThat(actual.getProduct(), is(expected.getProduct()));
    assertValidBillingPriceRecurringResponse(actual.getRecurring(), expected.getRecurring());
    assertThat(actual.getTiers(), hasSize(1));
    assertValidBillingPriceTierResponse(actual.getTiers().get(0), expected.getTiers().get(0));
  }

  private void assertValidBillingPriceRecurringResponse(BillingPriceRecurringResponse actual, Recurring expected) {
    assertThat(actual.getInterval(), is(expected.getInterval()));
    assertThat(actual.getIntervalCount(), is(expected.getIntervalCount()));
  }

  private void assertValidBillingPriceTierResponse(BillingPriceTierResponse actual, Tier expected) {
    assertThat(actual.getFlatAmount(), is(expected.getFlatAmount()));
    assertThat(actual.getUnitAmountDecimal(), is(expected.getUnitAmountDecimal()));
    assertThat(actual.getUpTo(), is(expected.getUpTo()));
  }
}