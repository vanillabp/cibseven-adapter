package io.vanillabp.cibseven.springboot.webapps.legacy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.env.MockEnvironment;

import io.vanillabp.cibseven.springboot.webapps.CibSevenWebclientSecret;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The secret of the webclient signs the webclient's login tokens. The classic web applications
 * do not sign tokens. So with only the classic ones the secret is not needed, and with both it
 * is.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenLegacyWebappsNeedNoSecretTest {

  @Test
  @DisplayName("With 'legacy' the start goes on without a secret")
  public void legacyNeedsNoSecret() {

    final var environment = new MockEnvironment().withProperty("vanillabp.webapps.ui", "legacy");

    assertDoesNotThrow(() -> new CibSevenWebclientSecret().postProcessEnvironment(environment, null));

  }

  @Test
  @DisplayName("With 'both' the start ends without a secret")
  public void bothNeedsTheSecret() {

    final var environment = new MockEnvironment().withProperty("vanillabp.webapps.ui", "both");

    assertThrows(
        IllegalStateException.class,
        () -> new CibSevenWebclientSecret().postProcessEnvironment(environment, null));

  }

}
