package io.vanillabp.cibseven.springboot.webapps;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@value CibSevenWebappsUi#KEY}, the choice of the web applications for the whole
 * application.
 *
 * <p>
 * The choice is read before any bean exists, by {@link CibSevenWebappsUi#of}, because it
 * decides which auto-configurations of CIB seven run. The platform ends the start for every
 * key below <code>vanillabp</code> which no {@code @ConfigurationProperties} class binds. So
 * this class binds the key as well. Where this module is missing, nothing binds it, and the
 * start ends for a key which would change nothing.
 * </p>
 */
@ConfigurationProperties("vanillabp.webapps")
public class CibSevenWebappsUiProperties {

  /**
   * Spring Boot builds it once and fills it through the setter.
   */
  public CibSevenWebappsUiProperties() {

  }

  /**
   * Which web applications of CIB seven the application serves: 'webclient' (the new webclient
   * at /webapp), 'legacy' (the classic Cockpit, Tasklist and Admin at /camunda, from the module
   * cibseven-adapter-spring-boot-webapps-legacy) or 'both'.
   */
  private CibSevenWebappsUi ui = CibSevenWebappsUi.DEFAULT;

  /**
   * Which web applications of CIB seven the application serves.
   *
   * @return The choice, the webclient unless the application chose something else
   */
  public CibSevenWebappsUi getUi() {
    return ui;
  }

  /**
   * Which web applications of CIB seven the application serves.
   *
   * @param ui The choice
   */
  public void setUi(
      final CibSevenWebappsUi ui) {
    this.ui = ui;
  }

}
