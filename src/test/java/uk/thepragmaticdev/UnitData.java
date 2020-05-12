package uk.thepragmaticdev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import uk.thepragmaticdev.account.Account;

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

  protected Account account() {
    var account = new Account();
    account.setUsername("username");
    account.setFullName("fullName");
    account.setEmailSubscriptionEnabled(true);
    account.setBillingAlertEnabled(false);
    account.setCreatedDate(OffsetDateTime.now(ZoneOffset.UTC));
    return account;
  }

}
