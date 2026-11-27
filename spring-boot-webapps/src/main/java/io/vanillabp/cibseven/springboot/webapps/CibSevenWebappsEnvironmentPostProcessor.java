package io.vanillabp.cibseven.springboot.webapps;

import java.util.HashMap;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Switches off Camunda's own engine auto-configuration, which arrives with the webapp
 * starter this module depends on.
 *
 * <p>
 * That auto-configuration always builds a {@code ProcessEngineFactoryBean}. So an
 * application would end up with two engines on one datasource. One is the engine VanillaBP
 * built and configured. The other is one Camunda built from <code>camunda.bpm.*</code>.
 * Both would run a job executor, and the second one would take the jobs of the first. The
 * webapps themselves do not need it. They find engines through the
 * {@code RuntimeContainerDelegate}, and {@link CibSevenWebappsRegistration} registers
 * VanillaBP's engines there.
 * </p>
 *
 * <p>
 * The value is added as the LAST property source, so an application can still see and
 * override it. Setting it to <code>true</code> is a defect, not a choice. So the start
 * fails with a message which says why.
 * </p>
 *
 * <p>
 * Where the webclient of CIB seven is served, the post processor also sets its provider to
 * the one which calls the engines of this application directly. The application can set
 * another one. See {@link CibSevenWebappsUi} for which web applications are served.
 * </p>
 */
public class CibSevenWebappsEnvironmentPostProcessor implements EnvironmentPostProcessor {

  /**
   * Spring Boot builds the class from <code>spring.factories</code>, before the context
   * exists.
   */
  public CibSevenWebappsEnvironmentPostProcessor() {

  }

  static final String CAMUNDA_BPM_ENABLED = "camunda.bpm.enabled";

  /** The key which tells CIB seven's webclient how it reaches the engines. */
  static final String WEBCLIENT_BPM_PROVIDER = "cibseven.webclient.bpm.provider";

  /** The provider of the webclient which calls the engines of this application directly. */
  static final String DIRECT_PROVIDER = "org.cibseven.webapp.providers.SevenDirectProvider";

  /**
   * The adapter's check for keys of version 1 skips a source named
   * <code>vanillabp-&lt;adapter type&gt;-webapps</code>. So the default is no key the
   * application wrote, also for the adapter of a fork of Camunda 7 in the same application.
   */
  static final String PROPERTY_SOURCE_NAME = "vanillabp-cibseven-webapps";

  @Override
  public void postProcessEnvironment(
      final ConfigurableEnvironment environment,
      final SpringApplication application) {

    final var configured = environment.getProperty(CAMUNDA_BPM_ENABLED);

    if (Boolean.parseBoolean(configured)) {

      throw new IllegalStateException(
          """
              '%s' is set to true, but VanillaBP builds and owns the CIB seven engines. \
              Camunda's engine auto-configuration would add a second engine on the same \
              datasource, whose job executor would acquire the jobs of the first one. \
              Remove the property (the webapps do not need it) and configure the engine \
              at 'vanillabp.adapters.<id>.*' instead."""
              .formatted(CAMUNDA_BPM_ENABLED));

    }

    final var defaults = new HashMap<String, Object>();
    if (configured == null) {
      defaults.put(CAMUNDA_BPM_ENABLED, "false");
    }

    // The webclient's default provider talks to the engine REST API at
    // http://localhost:8080/engine-rest, which an application of VanillaBP does not have. The
    // direct provider calls the engines in this application, which it finds in the runtime
    // container, where CibSevenWebappsRegistration put them
    final var ui = CibSevenWebappsUi.of(environment);
    if (ui.servesWebclient() && (environment.getProperty(WEBCLIENT_BPM_PROVIDER) == null)) {
      defaults.put(WEBCLIENT_BPM_PROVIDER, DIRECT_PROVIDER);
    }

    if (defaults.isEmpty()) {
      return;
    }

    environment
        .getPropertySources()
        .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));

  }

}
