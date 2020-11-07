package uk.thepragmaticdev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.stripe.model.Plan;
import com.stripe.model.Plan.Tier;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import uk.thepragmaticdev.account.Account;
import uk.thepragmaticdev.text.perspective.AttributeScore;
import uk.thepragmaticdev.text.perspective.AttributeScores;
import uk.thepragmaticdev.text.perspective.Score;
import uk.thepragmaticdev.text.perspective.SpanScore;
import uk.thepragmaticdev.text.perspective.dto.response.AnalyseCommentResponse;

public abstract class UnitData {

  @Autowired
  private ObjectMapper mapper;

  /**
   * Converts a Spring Page object to list of objects of type T.
   * 
   * @param <T>  The type of object in page
   * @param body The response json string
   * @param type The type of object in page
   * @return A list of objects of type T
   * @throws Exception If invalid json string
   */
  protected <T> List<T> pageToList(String body, Class<T> type) throws Exception {
    var object = new JsonParser().parse(body).getAsJsonObject();
    var elements = object.getAsJsonArray("content");
    var logs = new ArrayList<T>();
    for (JsonElement element : elements) {
      T log = mapper.readValue(element.toString(), type);
      logs.add(log);
    }
    return logs;
  }

  protected AnalyseCommentResponse analyseCommentResponse() {
    var response = new AnalyseCommentResponse();
    response.setAttributeScores(attributeScores());
    response.setLanguages(List.of("en"));
    response.setDetectedLanguages(List.of("en"));
    return response;
  }

  private AttributeScores attributeScores() {
    var attributeScores = new AttributeScores();
    attributeScores.setProfanity(attributeScore());
    attributeScores.setToxicity(attributeScore());
    return attributeScores;
  }

  private AttributeScore attributeScore() {
    var attributeScore = new AttributeScore();
    attributeScore.setSpanScores(List.of(spanScore()));
    attributeScore.setSummaryScore(score());
    return attributeScore;
  }

  private SpanScore spanScore() {
    return new SpanScore(1, 2, score());
  }

  private Score score() {
    return new Score(0.9, "type");
  }

  protected Account account() {
    var account = new Account();
    account.setUsername("username");
    account.setFullName("fullName");
    account.setEmailSubscriptionEnabled(true);
    account.setBillingAlertEnabled(false);
    account.setCreatedDate(OffsetDateTime.now(ZoneOffset.UTC));
    return account;
  }

  protected Plan plan() {
    var plan = new Plan();
    plan.setId("planId");
    plan.setCurrency("currency");
    plan.setNickname("starter");
    plan.setProduct("id");
    plan.setInterval("year");
    plan.setIntervalCount(1L);
    plan.setTiers(List.of(tier()));
    return plan;
  }

  private Tier tier() {
    var tier = new Tier();
    tier.setFlatAmount(1L);
    tier.setUnitAmountDecimal(new BigDecimal(0.2));
    tier.setUpTo(10L);
    return tier;
  }
}
