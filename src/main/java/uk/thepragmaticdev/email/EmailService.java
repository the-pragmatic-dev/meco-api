package uk.thepragmaticdev.email;

import net.sargue.mailgun.Configuration;
import net.sargue.mailgun.Mail;
import net.sargue.mailgun.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private Configuration configuration;

  @Autowired
  public EmailService(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * TODO.
   * 
   * @param to      TODO
   * @param subject TODO
   * @param text    TODO
   * @return
   */
  public Response send(String to, String subject, String text) {
    return Mail.using(configuration) //
        .to(to) //
        .subject(subject) //
        .text(text) //
        .build() //
        .send();
  }
}