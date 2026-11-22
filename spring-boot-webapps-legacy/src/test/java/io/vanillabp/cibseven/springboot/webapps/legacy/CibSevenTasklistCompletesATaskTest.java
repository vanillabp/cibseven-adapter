package io.vanillabp.cibseven.springboot.webapps.legacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A user completes a task in the classic Tasklist. The service task after it is served by
 * VanillaBP, so it only runs if the completion happened in the engine VanillaBP built.
 */
@ExtendWith(SuppressOutputExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:tasklist-task;DB_CLOSE_DELAY=-1", "vanillabp.workflow-modules.c7-webapps-test.adapters.cibseven.resources-location=classpath*:webapps-task", "vanillabp.adapters.cibseven.webapps.admin-user.id=demo", "vanillabp.adapters.cibseven.webapps.admin-user.password=demo"
    })
@ActiveProfiles("webapps-task")
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class CibSevenTasklistCompletesATaskTest {

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
  @DisplayName("A task completed in the Tasklist moves the workflow on in VanillaBP's engine")
  public void completingInTheTasklistRunsTheWiring() throws Exception {

    transaction.executeWithoutResult(status -> service.startWorkflow("4712"));
    awaitUntil(
        () -> repository.findById("4712").map(a -> a.getReviewTaskId() != null).orElse(false),
        "VanillaBP should be told about the user task");
    final var taskId = transaction
        .execute(status -> repository.findById("4712").orElseThrow().getReviewTaskId());

    final var rest = RestClient.builder().baseUrl("http://localhost:"
        + port)
        .defaultStatusHandler(status -> true, (
            request,
            response) -> {
          // the status is what the test looks at
        })
        .build();

    // the first response hands out the CSRF token, the login the session
    final var page = rest.get().uri("/camunda/app/tasklist/"
        + ENGINE
        + "/").retrieve()
        .toBodilessEntity();
    final var cookies = page.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertNotNull(cookies, "the Tasklist should hand out its cookies");
    final var csrfToken = cookies.stream()
        .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
        .map(cookie -> cookie.substring("XSRF-TOKEN=".length(), cookie.indexOf(';')))
        .findFirst()
        .orElseThrow();
    final var credentials = new LinkedMultiValueMap<String, String>();
    credentials.add("username", "demo");
    credentials.add("password", "demo");
    final var login = rest.post().uri("/camunda/api/admin/auth/user/"
        + ENGINE
        + "/login/tasklist")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .header("X-XSRF-TOKEN", csrfToken)
        .header(HttpHeaders.COOKIE, cookieHeader(cookies))
        .body(credentials)
        .retrieve().toEntity(String.class);
    assertEquals(HttpStatus.OK, login.getStatusCode(), login::getBody);
    // the login starts a new session. Its CSRF token comes with the next response
    final var afterLogin = login.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertNotNull(afterLogin, "the login should start a session");
    final var sessionCookies = cookieHeader(afterLogin);
    final var tasks = rest.get().uri("/camunda/api/engine/engine/"
        + ENGINE
        + "/task")
        .header(HttpHeaders.COOKIE, sessionCookies)
        .retrieve().toEntity(String.class);
    assertEquals(HttpStatus.OK, tasks.getStatusCode(), tasks::getBody);
    assertTrue(tasks.getBody().contains(taskId), () -> "the Tasklist should list the task: "
        + tasks.getBody());
    final var sessionCsrfToken = tasks.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
        .filter(cookie -> cookie.startsWith("XSRF-TOKEN="))
        .map(cookie -> cookie.substring("XSRF-TOKEN=".length(), cookie.indexOf(';')))
        .findFirst()
        .orElseThrow();

    final var complete = rest.post()
        .uri("/camunda/api/engine/engine/"
            + ENGINE
            + "/task/"
            + taskId
            + "/complete")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-XSRF-TOKEN", sessionCsrfToken)
        .header(HttpHeaders.COOKIE, sessionCookies)
        .body("{}")
        .retrieve().toEntity(String.class);
    assertTrue(complete.getStatusCode().is2xxSuccessful(),
        () -> complete.getStatusCode()
            + " "
            + complete.getBody());

    awaitUntil(
        () -> repository.findById("4712").map(WebappsTaskAggregate::isArchived).orElse(false),
        "the service task after the user task should run in VanillaBP's engine");

  }

  private static String cookieHeader(
      final java.util.List<String> setCookies) {

    return String.join("; ", setCookies.stream()
        .map(cookie -> cookie.contains(";") ? cookie.substring(0, cookie.indexOf(';')) : cookie)
        .toList());

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
