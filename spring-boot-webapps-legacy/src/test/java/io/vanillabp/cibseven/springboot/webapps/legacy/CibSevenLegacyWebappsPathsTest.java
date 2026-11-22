package io.vanillabp.cibseven.springboot.webapps.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.client.RestClient;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * CIB seven has two keys for the paths. <code>camunda.bpm.webapp.application-path</code> moves
 * the new webclient. With Camunda 7 the same key moved the classic web applications, so an
 * application of VanillaBP 1 may have it. <code>camunda.bpm.webapp.legacy-application-path</code>
 * moves the classic web applications. The wiki promises both, and this test holds them.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "vanillabp.webapps.ui=both", "spring.datasource.url=jdbc:h2:mem:legacy-webapps-paths;DB_CLOSE_DELAY=-1", "camunda.bpm.webapp.application-path=/engine-ui", "camunda.bpm.webapp.legacy-application-path=/classic"
    })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenLegacyWebappsPathsTest {

  @LocalServerPort
  private int port;

  @Test
  @DisplayName("application-path moves the webclient, legacy-application-path the classic web applications")
  public void eachKeyMovesItsOwnWebapps() {

    final var rest = RestClient
        .builder()
        .baseUrl("http://localhost:"
            + port)
        .defaultStatusHandler(status -> true, (
            request,
            response) -> {
          // the status is what the test looks at
        })
        .build();

    final var classic = rest.get().uri("/classic/api/engine/engine/").retrieve().toEntity(String.class);
    assertEquals(HttpStatus.OK, classic.getStatusCode(), classic::getBody);
    assertTrue(classic.getBody().contains("vanillabp-cibseven-cibseven"), classic::getBody);
    assertEquals(
        HttpStatus.NOT_FOUND,
        rest.get().uri("/camunda/api/engine/engine/").retrieve().toBodilessEntity().getStatusCode(),
        "the classic web applications should have left their default path");

    assertEquals(
        HttpStatus.OK,
        rest.get().uri("/engine-ui/services/v1/engine").retrieve().toBodilessEntity().getStatusCode(),
        "the webclient should answer at the configured application path");
    assertEquals(
        HttpStatus.NOT_FOUND,
        rest.get().uri("/webapp/services/v1/engine").retrieve().toBodilessEntity().getStatusCode(),
        "the webclient should have left its default path");

  }

}
