package io.vanillabp.cibseven.springboot.webapps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The choice of the web applications: its default, its messages, and what it switches on. The
 * test class path holds no module <code>cibseven-adapter-spring-boot-webapps-legacy</code>, so
 * every test here runs as an application without it.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenWebappsUiTest {

  private static final String LEGACY_AUTO_CONFIGURATION = CibSevenWebappsUiFilter.LEGACY
      .iterator()
      .next();

  private static final String WEBCLIENT_AUTO_CONFIGURATION = "org.cibseven.webapp.SevenWebclientContext";

  private static final String SOME_OTHER_AUTO_CONFIGURATION = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration";

  @Test
  @DisplayName("Without the key the webclient is served, and only the webclient")
  public void theWebclientIsTheDefault() {

    final var ui = CibSevenWebappsUi.of(new MockEnvironment());

    assertEquals(CibSevenWebappsUi.WEBCLIENT, ui);
    assertTrue(ui.servesWebclient());
    assertFalse(ui.servesLegacy());

  }

  @Test
  @DisplayName("A value which is none of the three ends the start and lists the three")
  public void anUnknownValueEndsTheStart() {

    final var environment = new MockEnvironment().withProperty(CibSevenWebappsUi.KEY, "cockpit");

    final var refused = assertThrows(IllegalStateException.class, () -> CibSevenWebappsUi.of(environment));

    assertTrue(refused.getMessage().contains("'vanillabp.webapps.ui' is 'cockpit'"), refused::getMessage);
    assertTrue(refused.getMessage().contains("'webclient', 'legacy', 'both'"), refused::getMessage);
    assertTrue(refused.getMessage().contains("to get 'webclient'"), refused::getMessage);

  }

  @ParameterizedTest
  @ValueSource(strings = {
      "legacy", "both", " Both "
  })
  @DisplayName("A choice of the classic web applications without their module ends the start")
  public void theClassicWebappsNeedTheirModule(
      final String value) {

    final var environment = new MockEnvironment().withProperty(CibSevenWebappsUi.KEY, value);

    final var refused = assertThrows(IllegalStateException.class, () -> CibSevenWebappsUi.of(environment));

    assertTrue(refused.getMessage().contains("'"
        + value.trim()
        + "'"), refused::getMessage);
    assertTrue(
        refused.getMessage().contains("'io.vanillabp:cibseven-adapter-spring-boot-webapps-legacy'"),
        refused::getMessage);
    assertTrue(
        refused.getMessage().contains("https://github.com/vanillabp/cibseven-adapter/wiki/Web-applications"),
        refused::getMessage);

  }

  @Test
  @DisplayName("With their module the classic web applications may be chosen")
  public void theClassicWebappsWithTheirModule() {

    assertEquals(
        CibSevenWebappsUi.LEGACY,
        CibSevenWebappsUi.of(new MockEnvironment().withProperty(CibSevenWebappsUi.KEY, "legacy"), true));
    assertEquals(
        CibSevenWebappsUi.BOTH,
        CibSevenWebappsUi.of(new MockEnvironment().withProperty(CibSevenWebappsUi.KEY, "both"), true));

  }

  @Test
  @DisplayName("The filter leaves out the classic web applications by default, and nothing else")
  public void theFilterLeavesOutTheClassicWebapps() {

    final var filter = new CibSevenWebappsUiFilter();
    filter.setEnvironment(new MockEnvironment());

    final var matches = filter.match(new String[]{
        LEGACY_AUTO_CONFIGURATION, WEBCLIENT_AUTO_CONFIGURATION, SOME_OTHER_AUTO_CONFIGURATION, null
    }, null);

    // an entry an earlier filter removed is null, and stays removed
    assertArrayEquals(new boolean[]{
        false, true, true, false
    }, matches);

  }

  @Test
  @DisplayName("The webclient finds the engines of this application, not a REST API somewhere")
  public void theWebclientUsesTheDirectProvider() {

    final var environment = new MockEnvironment();

    new CibSevenWebappsEnvironmentPostProcessor().postProcessEnvironment(environment, null);

    assertEquals(
        CibSevenWebappsEnvironmentPostProcessor.DIRECT_PROVIDER,
        environment.getProperty(CibSevenWebappsEnvironmentPostProcessor.WEBCLIENT_BPM_PROVIDER));

  }

  @Test
  @DisplayName("A provider the application chose is left alone")
  public void aProviderOfTheApplicationIsKept() {

    final var environment = new MockEnvironment()
        .withProperty(CibSevenWebappsEnvironmentPostProcessor.WEBCLIENT_BPM_PROVIDER, "my.Provider");

    new CibSevenWebappsEnvironmentPostProcessor().postProcessEnvironment(environment, null);

    assertEquals("my.Provider",
        environment.getProperty(CibSevenWebappsEnvironmentPostProcessor.WEBCLIENT_BPM_PROVIDER));
    final var added = environment
        .getPropertySources()
        .get(CibSevenWebappsEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);
    assertNull(
        added == null
            ? null
            : added.getProperty(CibSevenWebappsEnvironmentPostProcessor.WEBCLIENT_BPM_PROVIDER));

  }

}
