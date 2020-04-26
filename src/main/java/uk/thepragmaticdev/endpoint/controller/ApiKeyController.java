package uk.thepragmaticdev.endpoint.controller;

import com.monitorjbl.json.JsonResult;
import com.monitorjbl.json.JsonView;
import com.monitorjbl.json.Match;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.thepragmaticdev.kms.ApiKey;
import uk.thepragmaticdev.kms.ApiKeyService;
import uk.thepragmaticdev.log.key.ApiKeyLog;

@RestController
@RequestMapping("/api-keys")
@CrossOrigin("*")
@Tag(name = "api-keys")
public class ApiKeyController {

  private ApiKeyService apiKeyService;

  @Autowired
  public ApiKeyController(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  /**
   * Find all keys owned by the authenticaed account.
   * 
   * @param principal The currently authenticated principal user
   * @return A list of all keys owned by the account
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.OK)
  public List<ApiKey> findAll(Principal principal) {
    return apiKeyService.findAll(principal.getName());
  }

  /**
   * Create a new key. This is the only time the key field will be visible in the
   * response for security purposes.
   * 
   * @param principal The currently authenticated principal user
   * @param apiKey    The new key to be created
   * @return A newly created key
   */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.CREATED)
  public ApiKey create(Principal principal, @Valid @RequestBody ApiKey apiKey) {
    var key = apiKeyService.create(principal.getName(), apiKey);
    return JsonResult.instance().use(JsonView.with(key).onClass(ApiKey.class, Match.match().include("key")))
        .returnValue();
  }

  /**
   * Update all mutable fields of a key owned by an authenticated account.
   * 
   * @param principal The currently authenticated principal user
   * @param id        The id of the key to be updated
   * @param apiKey    A key with the desired values
   * @return The updated key
   */
  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.OK)
  public ApiKey update(Principal principal, @PathVariable long id, @Valid @RequestBody ApiKey apiKey) {
    return apiKeyService.update(principal.getName(), id, apiKey);
  }

  /**
   * Delete a key owned by an authenticated account.
   * 
   * @param principal The currently authenticated principal user
   * @param id        The id of the key to be deleted
   */
  @DeleteMapping(value = "/{id}")
  @ResponseStatus(value = HttpStatus.NO_CONTENT)
  public void delete(Principal principal, @PathVariable long id) {
    apiKeyService.delete(principal.getName(), id);
  }

  /**
   * Find the latest logs for the requested key id.
   * 
   * @param pageable  The pagination information
   * @param principal The currently authenticated principal user
   * @param id        The id of the key requesting logs
   * @return
   */
  @GetMapping(value = "/{id}/logs", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(value = HttpStatus.OK)
  public Page<ApiKeyLog> log(Pageable pageable, Principal principal, @PathVariable long id) {
    return apiKeyService.log(pageable, principal.getName(), id);
  }

  /**
   * Download all logs for the requested key id as a CSV file.
   * 
   * @param response  The servlet response
   * @param principal The currently authenticated principal user
   * @param id        The id of the key requesting logs
   */
  @GetMapping(value = "/{id}/logs/download", produces = "text/csv")
  @ResponseStatus(value = HttpStatus.OK)
  public void downloadLog(HttpServletResponse response, Principal principal, @PathVariable long id) {
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "api-key.csv" + "\"");
    apiKeyService.downloadLog(response, principal.getName(), id);
  }

  /**
   * Counts the amount of keys owned by the authenticated account.
   * 
   * @param principal The currently authenticated principal user
   * @return A count of keys owned by the authenticated account
   */
  @GetMapping(value = "/count")
  @ResponseStatus(value = HttpStatus.OK)
  public long count(Principal principal) {
    return apiKeyService.count(principal.getName());
  }
}