package io.vanillabp.cibseven.springboot.webapps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cibseven.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.client.RestClient;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application which brings its own users leaves the <code>admin-user</code> section
 * out. Then this module creates nobody. This test checks that nobody is created and that the
 * webclient is served.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:c7-webapps-no-admin;DB_CLOSE_DELAY=-1")
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenWebappsWithoutAdminUserTest {

  @LocalServerPort
  private int port;

  @Autowired
  private ProcessEngine engine;

  @Test
  @DisplayName("Without a configured administrator no user is created")
  public void noUserIsCreated() {

    assertEquals(
        0,
        engine
            .getIdentityService()
            .createUserQuery()
            .count());

  }

  @Test
  @DisplayName("The webclient is served nevertheless")
  public void webappsAreServedNevertheless() {

    assertEquals(
        HttpStatus.OK,
        RestClient
            .builder()
            .baseUrl("http://localhost:"
                + port)
            .build()
            .get()
            .uri("/webapp/")
            .retrieve()
            .toBodilessEntity()
            .getStatusCode());

  }

}
