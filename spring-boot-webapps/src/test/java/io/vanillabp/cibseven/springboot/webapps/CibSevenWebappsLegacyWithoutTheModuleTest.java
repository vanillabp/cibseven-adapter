package io.vanillabp.cibseven.springboot.webapps;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.builder.SpringApplicationBuilder;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application asks for the classic web applications and forgot their module. The start ends
 * before anything else happens, and the message names the module and the page of the wiki.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenWebappsLegacyWithoutTheModuleTest {

  @Test
  @DisplayName("'legacy' without the module ends the start with the coordinates of the module")
  public void theStartEnds() {

    final var refused = assertThrows(
        RuntimeException.class,
        () -> new SpringApplicationBuilder(TestApplication.class)
            .properties(
                "vanillabp.webapps.ui=legacy", "server.port=0",
                "spring.datasource.url=jdbc:h2:mem:webapps-legacy-without-module;DB_CLOSE_DELAY=-1")
            .run()
            .close());

    var cause = (Throwable) refused;
    while (cause.getCause() != null) {
      cause = cause.getCause();
    }
    final var message = cause.getMessage();
    assertTrue(message.startsWith("'vanillabp.webapps.ui' is 'legacy'"), message);
    assertTrue(message.contains(CibSevenWebappsUi.LEGACY_MODULE), message);
    assertTrue(message.contains(CibSevenWebappsUi.WIKI_PAGE), message);

  }

}
