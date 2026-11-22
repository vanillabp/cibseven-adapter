package io.vanillabp.cibseven.springboot.webapps.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.client.RestClient;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Both kinds of web applications in one application. Each of them shows the engine VanillaBP
 * built.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "vanillabp.webapps.ui=both", "spring.datasource.url=jdbc:h2:mem:webapps-both;DB_CLOSE_DELAY=-1", "vanillabp.adapters.cibseven.webapps.admin-user.id=demo", "vanillabp.adapters.cibseven.webapps.admin-user.password=demo"
    })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenWebappsBothTest {

  private static final String ENGINE = "vanillabp-cibseven-cibseven";

  /** The webclient reads the login as a polymorphic type, and its UI names this one. */
  private static final String STANDARD_LOGIN = "org.cibseven.webapp.auth.rest.StandardLogin";

  @LocalServerPort
  private int port;

  @Test
  @DisplayName("The webclient and the classic web applications both show VanillaBP's engine")
  public void bothShowTheEngine() {

    final var rest = RestClient.builder().baseUrl("http://localhost:"
        + port)
        .defaultStatusHandler(status -> true, (
            request,
            response) -> {
          // the status is what the test looks at
        })
        .build();

    final var webclient = rest.get().uri("/webapp/services/v1/engine").retrieve()
        .toEntity(String.class);
    assertEquals(HttpStatus.OK, webclient.getStatusCode(), webclient::getBody);
    assertTrue(webclient.getBody().contains(ENGINE), webclient::getBody);
    assertEquals(HttpStatus.OK, rest.get().uri("/webapp/").retrieve().toBodilessEntity()
        .getStatusCode());
    final var login = rest.post().uri("/webapp/services/v1/auth/login")
        .header("X-Process-Engine", ENGINE)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("type", STANDARD_LOGIN, "username", "demo", "password", "demo"))
        .retrieve().toEntity(String.class);
    assertEquals(HttpStatus.OK, login.getStatusCode(), login::getBody);
    assertTrue(login.getBody().contains("authToken"), login::getBody);

    final var legacy = rest.get().uri("/camunda/api/engine/engine/").retrieve()
        .toEntity(String.class);
    assertEquals(HttpStatus.OK, legacy.getStatusCode(), legacy::getBody);
    assertEquals("[{\"name\":\""
        + ENGINE
        + "\"}]", legacy.getBody());
    assertEquals(HttpStatus.OK, rest.get().uri("/camunda/app/tasklist/"
        + ENGINE
        + "/").retrieve()
        .toBodilessEntity().getStatusCode());

  }

}
