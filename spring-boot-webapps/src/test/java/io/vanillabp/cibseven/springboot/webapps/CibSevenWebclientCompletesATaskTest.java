package io.vanillabp.cibseven.springboot.webapps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user completes a task in the new webclient. The service task after it is served by
 * VanillaBP, so it only runs if the completion happened in the engine VanillaBP built.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "vanillabp.webapps.ui=webclient", "spring.datasource.url=jdbc:h2:mem:webclient-task;DB_CLOSE_DELAY=-1", "vanillabp.workflow-modules.c7-webapps-test.adapters.cibseven.resources-location=classpath*:webapps-task", "vanillabp.adapters.cibseven.webapps.admin-user.id=demo", "vanillabp.adapters.cibseven.webapps.admin-user.password=demo"
    })
@ActiveProfiles("webapps-task")
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenWebclientCompletesATaskTest {

  private static final String ENGINE = "vanillabp-cibseven-cibseven";

  @LocalServerPort
  private int port;

  @Autowired
  private WebappsTaskWorkflowService service;

  @Autowired
  private WebappsTaskRepository repository;

  @Autowired
  private TransactionTemplate transaction;

  @Test
  @DisplayName("A task completed in the webclient moves the workflow on in VanillaBP's engine")
  public void completingInTheWebclientRunsTheWiring() throws Exception {

    transaction.executeWithoutResult(status -> service.startWorkflow("4711"));
    awaitUntil(
        () -> repository.findById("4711").map(a -> a.getReviewTaskId() != null).orElse(false),
        "VanillaBP should be told about the user task");
    final var taskId = transaction
        .execute(status -> repository.findById("4711").orElseThrow().getReviewTaskId());

    final var rest = RestClient.builder().baseUrl("http://localhost:"
        + port)
        .defaultStatusHandler(status -> true, (
            request,
            response) -> {
          // the status is what the test looks at
        })
        .build();
    final var token = CibSevenWebclientTest.login(rest, ENGINE, "demo", "demo");

    // the task as the webclient lists it
    final var task = rest.get().uri("/webapp/services/v1/task/{id}", taskId)
        .header("X-Process-Engine", ENGINE)
        .header(HttpHeaders.AUTHORIZATION, token)
        .retrieve().toEntity(String.class);
    assertEquals(HttpStatus.OK, task.getStatusCode(), task::getBody);
    assertTrue(task.getBody().contains("Review"), task::getBody);

    final var submit = rest.post().uri("/webapp/services/v1/task/submit/{id}", taskId)
        .header("X-Process-Engine", ENGINE)
        .header(HttpHeaders.AUTHORIZATION, token)
        .retrieve().toEntity(String.class);
    assertTrue(submit.getStatusCode().is2xxSuccessful(),
        () -> submit.getStatusCode()
            + " "
            + submit.getBody());

    awaitUntil(
        () -> repository.findById("4711").map(WebappsTaskAggregate::isArchived).orElse(false),
        "the service task after the user task should run in VanillaBP's engine");

  }

  private void awaitUntil(
      final Supplier<Boolean> condition,
      final String whatDidNotHappen) throws Exception {

    final var deadline = System.currentTimeMillis() + 60_000;
    while (!Boolean.TRUE.equals(transaction.execute(status -> condition.get()))) {
      if (System.currentTimeMillis() > deadline) {
        throw new AssertionError(whatDidNotHappen);
      }
      Thread.sleep(100);
    }

  }

}
