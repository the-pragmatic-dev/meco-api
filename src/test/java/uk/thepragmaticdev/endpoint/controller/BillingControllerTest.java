package uk.thepragmaticdev.endpoint.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Plan;
import com.stripe.model.Plan.Tier;
import com.stripe.model.PlanCollection;
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
import uk.thepragmaticdev.billing.dto.response.BillingPlanResponse;
import uk.thepragmaticdev.billing.dto.response.BillingPlanTierResponse;

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
  void shouldMapToListOfBillingPlanResponse() throws Exception {
    var expected = List.of(plan());
    var planCollection = mock(PlanCollection.class);
    when(planCollection.getData()).thenReturn(expected);
    when(billingService.findAllPlans()).thenReturn(planCollection);

    var body = mvc.perform(//
        MockMvcRequestBuilders.get("/v1/billing/plans")//
            .accept(MediaType.APPLICATION_JSON)//
    ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    var response = mapper.readValue(body, new TypeReference<List<BillingPlanResponse>>() {
    });

    assertThat(response, hasSize(1));
    assertValidBillingPlanResponse(response.get(0), expected.get(0));
  }

  private void assertValidBillingPlanResponse(BillingPlanResponse actual, Plan expected) {
    assertThat(actual.getId(), is(expected.getId()));
    assertThat(actual.getCurrency(), is(expected.getCurrency()));
    assertThat(actual.getNickname(), is(expected.getNickname()));
    assertThat(actual.getProduct(), is(expected.getProduct()));
    assertThat(actual.getInterval(), is(expected.getInterval()));
    assertThat(actual.getIntervalCount(), is(expected.getIntervalCount()));
    assertValidBillingPlanTierResponse(actual.getTiers(), expected.getTiers());
  }

  private void assertValidBillingPlanTierResponse(List<BillingPlanTierResponse> actual, List<Tier> expected) {
    for (int i = 0; i < expected.size(); i++) {
      assertThat(actual.get(i).getFlatAmount(), is(expected.get(i).getFlatAmount().intValue()));
      assertThat(actual.get(i).getUnitAmountDecimal(), is(expected.get(i).getUnitAmountDecimal().doubleValue()));
      assertThat(actual.get(i).getUpTo(), is(expected.get(i).getUpTo().intValue()));
    }
  }
}