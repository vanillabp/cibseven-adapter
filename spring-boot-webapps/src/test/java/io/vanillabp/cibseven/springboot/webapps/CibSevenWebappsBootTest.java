package io.vanillabp.cibseven.springboot.webapps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.cibseven.bpm.container.RuntimeContainerDelegate;
import org.cibseven.bpm.engine.spring.ProcessEngineFactoryBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.web.client.RestClient;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * Boots an application with this module and nothing set for the web applications, and asks
 * them what they serve.
 *
 * <p>
 * The interesting assertion is the engine NAME. It proves that the webclient talks to the engine
 * VanillaBP built for the adapter id. It does not talk to an engine which Camunda's own
 * auto-configuration created.
 * </p>
 *
 * <p>
 * Camunda 7 serves Cockpit, Tasklist and Admin at <code>/camunda</code>. CIB seven 3.0 serves its
 * new webclient at <code>/webapp</code>, and the classic web applications only come with the
 * module <code>cibseven-adapter-spring-boot-webapps-legacy</code>. Its tests ask for
 * <code>/camunda</code>.
 * </p>
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:c7-webapps-boot;DB_CLOSE_DELAY=-1", "vanillabp.adapters.cibseven.webapps.admin-user.id=demo", "vanillabp.adapters.cibseven.webapps.admin-user.password=demo", "vanillabp.adapters.cibseven.webapps.admin-user.first-name=Demo", "vanillabp.adapters.cibseven.webapps.admin-user.last-name=User"
    })
// the runtime container is JVM-wide. So this context releases its engine when the class
// is done, and does not leave it there for the next test class
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenWebappsBootTest {

  private static final String ENGINE = "vanillabp-cibseven-cibseven";

  @LocalServerPort
  private int port;

  @Autowired
  private ApplicationContext context;

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
  @DisplayName("The engine auto-configuration of Camunda is switched off in a booted application")
  public void camundasOwnAutoConfigurationIsOff() {

    // the unit test of the post processor proves what it adds. This one proves that it
    // RUNS. The interface and the spring.factories key have to name the same type. We
    // measured it: naming the old key while implementing the new interface fails the boot
    // with "not assignable to factory type". So this test catches a half-done migration
    assertEquals(
        "false",
        context.getEnvironment().getProperty(CibSevenWebappsEnvironmentPostProcessor.CAMUNDA_BPM_ENABLED),
        "without it Camunda's starter builds a second engine on the same datasource");

  }

  @Test
  @DisplayName("Without a choice the webclient is served at /webapp, and nothing at /camunda")
  public void theWebclientIsTheDefault() {

    final var index = rest
        .get()
        .uri("/webapp/")
        .retrieve()
        .toEntity(String.class);
    assertEquals(HttpStatus.OK, index.getStatusCode());
    assertTrue(
        index.getBody().contains("<html") || index.getBody().contains("<!DOCTYPE"),
        index::getBody);

    assertEquals(
        HttpStatus.NOT_FOUND,
        rest
            .get()
            .uri("/camunda/app/cockpit/"
                + ENGINE
                + "/")
            .retrieve()
            .toBodilessEntity()
            .getStatusCode(),
        "the classic web applications need a module of their own");

  }

  @Test
  @DisplayName("It serves the engine VanillaBP built, and only that one")
  public void theEngineIsTheOneVanillaBpBuilt() {

    final var engines = rest
        .get()
        .uri("/webapp/services/v1/engine")
        .retrieve()
        .toEntity(String.class);

    assertEquals(HttpStatus.OK, engines.getStatusCode(), engines::getBody);
    assertTrue(engines.getBody().contains(ENGINE), engines::getBody);

    // Camunda's engine auto-configuration would have added a second engine on the
    // application's datasource. Its job executor would take VanillaBP's jobs
    assertEquals(
        0,
        context.getBeanNamesForType(ProcessEngineFactoryBean.class).length,
        "Camunda's engine auto-configuration must not run");
    assertEquals(
        1,
        context
            .getBeanProvider(io.vanillabp.cibseven.springboot.engine.CibSevenEngineHolder.class)
            .stream()
            .count(),
        "VanillaBP builds one engine per adapter id, and there is one id here");
    assertTrue(
        RuntimeContainerDelegate.INSTANCE
            .get()
            .getProcessEngineService()
            .getProcessEngineNames()
            .contains(ENGINE));

  }

  @Test
  @DisplayName("The configured administrator can log in")
  public void theAdministratorCanLogIn() {

    assertNotNull(CibSevenWebclientTest.login(rest, ENGINE, "demo", "demo"));

  }

}
