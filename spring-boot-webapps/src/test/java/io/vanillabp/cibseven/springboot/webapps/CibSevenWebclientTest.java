package io.vanillabp.cibseven.springboot.webapps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.client.RestClient;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Only the new webclient: two adapter ids, the second one on a datasource of its own. The
 * webclient reaches both engines through its direct provider, without the engine REST API.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    classes = {
        TestApplication.class, CibSevenWebclientTest.TwoDataSources.class
    },
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "vanillabp.webapps.ui=webclient", "vanillabp.adapters.cibseven.data-source-name=dataSource", "vanillabp.adapters.c7-new.type=cibseven", "vanillabp.adapters.c7-new.data-source-name=c7NewDataSource", "vanillabp.prioritized-adapters=cibseven,c7-new", "vanillabp.adapters.cibseven.webapps.admin-user.id=demo", "vanillabp.adapters.cibseven.webapps.admin-user.password=demo", "vanillabp.adapters.c7-new.webapps.admin-user.id=other", "vanillabp.adapters.c7-new.webapps.admin-user.password=other"
    })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenWebclientTest {

  @Configuration
  public static class TwoDataSources {

    @Bean
    @Primary
    public DataSource dataSource() {

      return new SimpleDriverDataSource(
          new org.h2.Driver(), "jdbc:h2:mem:webclient-two-ids;DB_CLOSE_DELAY=-1", "sa", "");

    }

    @Bean
    public DataSource c7NewDataSource() {

      return new SimpleDriverDataSource(
          new org.h2.Driver(), "jdbc:h2:mem:webclient-two-ids-new;DB_CLOSE_DELAY=-1", "sa", "");

    }

  }

  /** The webclient reads the login as a polymorphic type, and its UI names this one. */
  static final String STANDARD_LOGIN = "org.cibseven.webapp.auth.rest.StandardLogin";

  @LocalServerPort
  private int port;

  private RestClient rest;

  @BeforeEach
  public void restClient() {

    rest = RestClient
        .builder()
        .baseUrl("http://localhost:"
            + port)
        .defaultStatusHandler(status -> true, (
            request,
            response) -> {
          // the status is what the tests look at
        })
        .build();

  }

  @Test
  @DisplayName("The webclient is served at /webapp, the classic web applications are not")
  public void onlyTheWebclientIsServed() {

    final var index = rest.get().uri("/webapp/").retrieve().toEntity(String.class);
    assertEquals(HttpStatus.OK, index.getStatusCode());
    assertTrue(index.getBody().contains("<html") || index.getBody().contains("<!DOCTYPE"),
        index::getBody);

    assertEquals(
        HttpStatus.NOT_FOUND,
        rest.get().uri("/camunda/app/cockpit/vanillabp-cibseven-cibseven/").retrieve()
            .toBodilessEntity().getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        rest.get().uri("/camunda/api/engine/engine/").retrieve().toBodilessEntity()
            .getStatusCode());

  }

  @Test
  @DisplayName("The webclient offers the engines of both adapter ids")
  public void bothEnginesAreOffered() {

    final var engines = rest.get().uri("/webapp/services/v1/engine").retrieve()
        .toEntity(String.class);

    assertEquals(HttpStatus.OK, engines.getStatusCode(), engines::getBody);
    assertTrue(engines.getBody().contains("vanillabp-cibseven-cibseven"), engines::getBody);
    assertTrue(engines.getBody().contains("vanillabp-cibseven-c7-new"), engines::getBody);

  }

  @Test
  @DisplayName("A user logs in against the identity service of the engine they choose")
  public void loginIsCheckedByTheChosenEngine() {

    assertNotNull(login(rest, "vanillabp-cibseven-cibseven", "demo", "demo"));
    assertNotNull(login(rest, "vanillabp-cibseven-c7-new", "other", "other"));

    // 'demo' exists only in the engine of the first adapter id
    final var wrongEngine = rest.post().uri("/webapp/services/v1/auth/login")
        .header("X-Process-Engine", "vanillabp-cibseven-c7-new")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("type", STANDARD_LOGIN, "username", "demo", "password", "demo"))
        .retrieve().toEntity(String.class);
    assertTrue(wrongEngine.getStatusCode().is4xxClientError(),
        () -> wrongEngine.getStatusCode()
            + " "
            + wrongEngine.getBody());

  }

  /**
   * Logs in at the webclient.
   *
   * @return The value of the header 'Authorization' the webclient expects in every later
   *         request. It starts with 'Bearer ' already
   */
  @SuppressWarnings("unchecked")
  static String login(
      final RestClient rest,
      final String engine,
      final String username,
      final String password) {

    final var login = rest.post().uri("/webapp/services/v1/auth/login")
        .header("X-Process-Engine", engine)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("type", STANDARD_LOGIN, "username", username, "password", password))
        .retrieve().toEntity(Map.class);
    assertEquals(HttpStatus.OK, login.getStatusCode(), () -> ""
        + login.getBody());
    final var token = (String) login.getBody().get("authToken");
    assertNotNull(token, () -> "no token in "
        + login.getBody());
    return token;

  }

}
