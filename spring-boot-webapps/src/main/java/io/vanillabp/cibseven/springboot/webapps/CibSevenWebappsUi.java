package io.vanillabp.cibseven.springboot.webapps;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * Which web applications of CIB seven the application serves. It is a choice of the whole
 * application, not of one adapter id: both kinds of web applications find their engines in
 * the same runtime container, so each of them shows every engine registered there.
 *
 * <p>
 * The default is CIB seven's new webclient. The classic Cockpit, Tasklist and Admin are in the
 * module <code>cibseven-adapter-spring-boot-webapps-legacy</code>, which the application adds
 * itself. A choice which needs that module ends the start where it is missing. See
 * {@code DECISIONS.pending/cibseven-3.0.md} in the repository for why.
 * </p>
 */
public enum CibSevenWebappsUi {

  /** CIB seven's new webclient, at <code>/webapp</code>. */
  WEBCLIENT(true, false),

  /** The classic Cockpit, Tasklist and Admin, at <code>/camunda</code>. */
  LEGACY(false, true),

  /** Both of them. */
  BOTH(true, true);

  /** The key which selects the web applications. */
  public static final String KEY = "vanillabp.webapps.ui";

  /** The value used when the application does not set the key. */
  public static final CibSevenWebappsUi DEFAULT = WEBCLIENT;

  /** The coordinates of the module which brings the classic web applications. */
  public static final String LEGACY_MODULE = "io.vanillabp:cibseven-adapter-spring-boot-webapps-legacy";

  /** The page of the wiki which says how to switch the classic web applications on. */
  public static final String WIKI_PAGE = "https://github.com/vanillabp/cibseven-adapter/wiki/Web-applications";

  /**
   * A class of the module {@link #LEGACY_MODULE}. It is named as text, because this module
   * must not depend on that one.
   */
  static final String LEGACY_MODULE_CLASS = "io.vanillabp.cibseven.springboot.webapps.legacy.CibSevenLegacyWebappsAutoConfiguration";

  private final boolean webclient;

  private final boolean legacy;

  CibSevenWebappsUi(
      final boolean webclient,
      final boolean legacy) {

    this.webclient = webclient;
    this.legacy = legacy;

  }

  /**
   * Whether the new webclient is served.
   *
   * @return <code>true</code> for <code>webclient</code> and <code>both</code>
   */
  public boolean servesWebclient() {
    return webclient;
  }

  /**
   * Whether the classic web applications are served.
   *
   * @return <code>true</code> for <code>legacy</code> and <code>both</code>
   */
  public boolean servesLegacy() {
    return legacy;
  }

  /**
   * The value as it is written in the configuration.
   *
   * @return <code>webclient</code>, <code>legacy</code> or <code>both</code>
   */
  public String value() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * Reads the choice of the application. The start ends where the value is none of the three,
   * or where it asks for the classic web applications and their module is missing. Each
   * message says what to do.
   *
   * @param environment The environment of the application
   * @return The choice, {@link #DEFAULT} if the key is not set
   */
  public static CibSevenWebappsUi of(
      final Environment environment) {

    return of(environment, ClassUtils.isPresent(LEGACY_MODULE_CLASS, CibSevenWebappsUi.class.getClassLoader()));

  }

  /**
   * Reads the choice of the application, see {@link #of(Environment)}.
   *
   * @param environment The environment of the application
   * @param legacyModuleIsPresent Whether the module {@link #LEGACY_MODULE} is on the class path
   * @return The choice, {@link #DEFAULT} if the key is not set
   */
  static CibSevenWebappsUi of(
      final Environment environment,
      final boolean legacyModuleIsPresent) {

    final var value = environment.getProperty(KEY);
    final var ui = parse(value);
    if (ui.servesLegacy() && !legacyModuleIsPresent) {
      throw new IllegalStateException(
          """
              '%s' is '%s', so the classic Cockpit, Tasklist and Admin should be served at \
              '/camunda'. They are in a module of their own, and it is not on the class path. \
              Add the dependency '%s', in the same version as 'cibseven-adapter-spring-boot-webapps'. \
              Or remove the key to use only CIB seven's new webclient at '/webapp'. %s says more."""
              .formatted(KEY, value.trim(), LEGACY_MODULE, WIKI_PAGE));
    }
    return ui;

  }

  private static CibSevenWebappsUi parse(
      final String value) {

    if ((value == null) || value.isBlank()) {
      return DEFAULT;
    }

    try {
      return valueOf(value
          .trim()
          .toUpperCase(Locale.ROOT));
    } catch (final IllegalArgumentException unknown) {
      throw new IllegalStateException(
          """
              '%s' is '%s', but it has to be one of %s. 'webclient' serves CIB seven's new \
              webclient at '/webapp'. 'legacy' serves the classic Cockpit, Tasklist and Admin at \
              '/camunda'. 'both' serves both. Leave the key out to get '%s'."""
              .formatted(
                  KEY,
                  value,
                  Arrays
                      .stream(values())
                      .map(ui -> "'"
                          + ui.value()
                          + "'")
                      .collect(Collectors.joining(", ")),
                  DEFAULT.value()), unknown);
    }

  }

}
