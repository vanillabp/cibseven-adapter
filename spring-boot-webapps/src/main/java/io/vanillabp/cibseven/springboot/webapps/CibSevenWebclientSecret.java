package io.vanillabp.cibseven.springboot.webapps;

import java.util.Base64;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Checks the secret of CIB seven's webclient while the application boots.
 *
 * <p>
 * The web application starter of CIB seven, which this module depends on, brings two things:
 * the classic web applications at <code>/camunda</code> and CIB seven's new webclient at
 * <code>/webapp</code>. The webclient signs its login tokens with the key
 * <code>cibseven.webclient.authentication.jwtSecret</code>. Without a valid one it refuses to
 * start, deep inside the context, with a message which names neither this module nor the key.
 * </p>
 *
 * <p>
 * A secret is not something an adapter may make up, because every instance of the application
 * has to sign with the same one. So this class only checks the key, by the same two rules the
 * webclient applies: at least 155 characters, and base64. If the key breaks one of them, the
 * start ends here with a message which names the key and says how to get a secret. Decision 6
 * in the repository's DECISIONS.md says why the module asks for the key and does not set it.
 * </p>
 */
public class CibSevenWebclientSecret implements EnvironmentPostProcessor {

  /** The key the webclient reads its secret from. */
  static final String JWT_SECRET = "cibseven.webclient.authentication.jwtSecret";

  /** The shortest secret the webclient accepts. */
  static final int MINIMUM_LENGTH = 155;

  /**
   * Spring Boot builds the class from <code>spring.factories</code>, before the context
   * exists.
   */
  public CibSevenWebclientSecret() {

  }

  @Override
  public void postProcessEnvironment(
      final ConfigurableEnvironment environment,
      final SpringApplication application) {

    final var secret = environment.getProperty(JWT_SECRET);

    if ((secret == null) || secret.isBlank()) {
      throw new IllegalStateException(
          """
              The CIB seven web applications need a secret for the login tokens of CIB seven's \
              webclient, and '%s' is not set. The webclient comes with CIB seven's web \
              application starter, which 'cibseven-adapter-spring-boot-webapps' depends on, and it \
              does not start without the secret. Set '%s' to a base64 text of at least %d \
              characters, for example the output of 'openssl rand -base64 120 | tr -d \"\\n\"'. \
              Every instance of the application needs the same secret, so keep it with the other \
              secrets of the application, not in its sources."""
              .formatted(JWT_SECRET, JWT_SECRET, MINIMUM_LENGTH));
    }

    if (secret.length() < MINIMUM_LENGTH) {
      throw new IllegalStateException(
          """
              '%s' is %d characters long, but CIB seven's webclient needs at least %d. Use a \
              longer base64 text, for example the output of 'openssl rand -base64 120 | tr -d \"\\n\"'."""
              .formatted(JWT_SECRET, secret.length(), MINIMUM_LENGTH));
    }

    try {
      Base64.getDecoder().decode(secret);
    } catch (final IllegalArgumentException notBase64) {
      throw new IllegalStateException(
          """
              '%s' is not a base64 text, but CIB seven's webclient decodes it as one: %s. Use the \
              output of 'openssl rand -base64 120 | tr -d \"\\n\"', for example."""
              .formatted(JWT_SECRET, notBase64.getMessage()), notBase64);
    }

  }

}
