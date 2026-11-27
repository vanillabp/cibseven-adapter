package io.vanillabp.cibseven.springboot.webapps.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * The module is on the class path, but the application chose only the webclient. Then the
 * classic web applications stay off, and the webclient is served as without the module.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "vanillabp.webapps.ui=webclient", "spring.datasource.url=jdbc:h2:mem:legacy-webapps-off;DB_CLOSE_DELAY=-1"
    })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenLegacyWebappsStayOffTest {

  @LocalServerPort
  private int port;

  @Test
  @DisplayName("With 'webclient' the classic web applications stay off, although their module is there")
  public void theClassicWebappsStayOff() {

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

    assertEquals(
        HttpStatus.NOT_FOUND,
        rest.get().uri("/camunda/api/engine/engine/").retrieve().toBodilessEntity().getStatusCode());
    assertEquals(
        HttpStatus.NOT_FOUND,
        rest.get().uri("/camunda/app/cockpit/vanillabp-cibseven-cibseven/").retrieve().toBodilessEntity()
            .getStatusCode());
    assertEquals(
        HttpStatus.OK,
        rest.get().uri("/webapp/services/v1/engine").retrieve().toBodilessEntity().getStatusCode());

  }

}
