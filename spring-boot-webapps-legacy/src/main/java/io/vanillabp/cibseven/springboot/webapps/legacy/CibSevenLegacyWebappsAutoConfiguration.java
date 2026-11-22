package io.vanillabp.cibseven.springboot.webapps.legacy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.core.env.Environment;

import io.vanillabp.cibseven.springboot.webapps.CibSevenWebappsAutoConfiguration;
import io.vanillabp.cibseven.springboot.webapps.CibSevenWebappsUi;

/**
 * Says at the start whether the classic Cockpit, Tasklist and Admin of this module are served.
 *
 * <p>
 * This module brings CIB seven's starter for the classic web applications and their UI files.
 * CIB seven's own auto-configuration serves them. Whether it runs is decided by
 * {@value CibSevenWebappsUi#KEY}, in the module <code>cibseven-adapter-spring-boot-webapps</code>.
 * That module finds this one by the name of this class. So this class stays where it is, even
 * though it only writes one line to the log.
 * </p>
 *
 * <p>
 * The line matters where the module is on the class path and the key says
 * <code>webclient</code>. Then the classic web applications stay off, and the log says how to
 * switch them on.
 * </p>
 */
@AutoConfiguration(after = CibSevenWebappsAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class CibSevenLegacyWebappsAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(CibSevenLegacyWebappsAutoConfiguration.class);

  /** The CIB seven key which moves the classic web applications away from their default path. */
  static final String LEGACY_APPLICATION_PATH = "camunda.bpm.webapp.legacy-application-path";

  /** Where the classic web applications are served by default. */
  static final String DEFAULT_LEGACY_APPLICATION_PATH = "/camunda";

  /**
   * Spring Boot builds the class while it reads the auto-configurations.
   *
   * @param environment The environment of the application, which holds the choice
   */
  public CibSevenLegacyWebappsAutoConfiguration(
      final Environment environment) {

    final var ui = CibSevenWebappsUi.of(environment);
    if (ui.servesLegacy()) {
      logger.info(
          """
              CibSeven: the classic Cockpit, Tasklist and Admin are served at '{}', because '{}' \
              is '{}'. CIB seven deprecated them. Its new webclient does the same work.""",
          environment.getProperty(LEGACY_APPLICATION_PATH, DEFAULT_LEGACY_APPLICATION_PATH),
          CibSevenWebappsUi.KEY,
          ui.value());
    } else {
      logger.info(
          """
              CibSeven: the module '{}' is on the class path, but '{}' is '{}'. So the classic \
              Cockpit, Tasklist and Admin stay off. Set the key to 'both' or 'legacy' to serve \
              them, or remove the module.""",
          CibSevenWebappsUi.LEGACY_MODULE,
          CibSevenWebappsUi.KEY,
          ui.value());
    }

  }

}
