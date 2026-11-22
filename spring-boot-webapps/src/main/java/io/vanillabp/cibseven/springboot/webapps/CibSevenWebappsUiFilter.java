package io.vanillabp.cibseven.springboot.webapps;

import java.util.Set;

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Leaves out the auto-configurations of the web applications the application did not choose
 * in {@value CibSevenWebappsUi#KEY}.
 *
 * <p>
 * Both kinds of web applications switch on with the same CIB seven key,
 * <code>camunda.bpm.webapp.enabled</code>, and the classic ones have no key of their own. So
 * a property cannot pick one of them. This filter removes the auto-configurations of the
 * other one before Spring Boot reads them.
 * </p>
 *
 * <p>
 * The classic ones come only with the module
 * <code>cibseven-adapter-spring-boot-webapps-legacy</code>. Where that module is on the class
 * path and the application chose <code>webclient</code>, this filter keeps them off.
 * </p>
 */
public class CibSevenWebappsUiFilter implements AutoConfigurationImportFilter, EnvironmentAware {

  /** The classic Cockpit, Tasklist and Admin, from the legacy starter. */
  static final Set<String> LEGACY = Set.of(
      "org.cibseven.bpm.spring.boot.starter.webapp.CamundaBpmLegacyWebappAutoConfiguration");

  /** The new webclient: its resources at the application path, its services and its modeler. */
  static final Set<String> WEBCLIENT = Set.of(
      "org.cibseven.bpm.spring.boot.starter.webapp.CamundaBpmWebappAutoConfiguration",
      "org.cibseven.webapp.SevenWebclientContext",
      "org.cibseven.modeler.config.ModelerAutoConfiguration");

  private Environment environment;

  /**
   * Spring Boot builds the class from <code>spring.factories</code>.
   */
  public CibSevenWebappsUiFilter() {

  }

  @Override
  public void setEnvironment(
      final Environment environment) {

    this.environment = environment;

  }

  @Override
  public boolean[] match(
      final String[] autoConfigurationClasses,
      final AutoConfigurationMetadata autoConfigurationMetadata) {

    final var ui = CibSevenWebappsUi.of(environment);
    final var result = new boolean[autoConfigurationClasses.length];
    for (var i = 0; i < autoConfigurationClasses.length; ++i) {
      final var name = autoConfigurationClasses[i];
      if (name == null) {
        // an earlier filter removed it already
        continue;
      }
      final var notChosen = (!ui.servesLegacy() && LEGACY.contains(name)) || (!ui.servesWebclient() && WEBCLIENT
          .contains(name));
      result[i] = !notChosen;
    }
    return result;

  }

}
