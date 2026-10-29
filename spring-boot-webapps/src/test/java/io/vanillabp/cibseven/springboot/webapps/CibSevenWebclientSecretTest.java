package io.vanillabp.cibseven.springboot.webapps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.env.MockEnvironment;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * CIB seven's webclient does not start without a secret for its login tokens. The module says
 * so while the application boots, and names the key, instead of letting the webclient fail
 * deep inside the context.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenWebclientSecretTest {

  private final CibSevenWebclientSecret check = new CibSevenWebclientSecret();

  private static String secretOfBytes(
      final int bytes) {

    return Base64.getEncoder().encodeToString(new byte[bytes]);

  }

  @Test
  @DisplayName("Without a secret the start ends with a message which names the key and the length")
  public void aMissingSecretIsNamed() {

    final var refused = assertThrows(
        IllegalStateException.class,
        () -> check.postProcessEnvironment(new MockEnvironment(), null));

    final var message = refused.getMessage();
    assertTrue(message.contains("'cibseven.webclient.authentication.jwtSecret' is not set"), () -> message);
    assertTrue(message.contains("at least 155 characters"), () -> message);
    assertTrue(message.contains("openssl rand -base64 120"), () -> message);

  }

  @Test
  @DisplayName("A secret shorter than the webclient accepts is refused with its length")
  public void aShortSecretIsRefused() {

    final var environment = new MockEnvironment()
        .withProperty(CibSevenWebclientSecret.JWT_SECRET, secretOfBytes(60));

    final var refused = assertThrows(
        IllegalStateException.class,
        () -> check.postProcessEnvironment(environment, null));

    assertTrue(refused.getMessage().contains("is 80 characters long"), refused::getMessage);

  }

  @Test
  @DisplayName("A secret which is not base64 is refused")
  public void aSecretWhichIsNotBase64IsRefused() {

    final var environment = new MockEnvironment()
        .withProperty(CibSevenWebclientSecret.JWT_SECRET, "!".repeat(200));

    final var refused = assertThrows(
        IllegalStateException.class,
        () -> check.postProcessEnvironment(environment, null));

    assertTrue(refused.getMessage().contains("is not a base64 text"), refused::getMessage);

  }

  @Test
  @DisplayName("A secret the webclient accepts lets the start go on")
  public void aGoodSecretPasses() {

    final var environment = new MockEnvironment()
        .withProperty(CibSevenWebclientSecret.JWT_SECRET, secretOfBytes(120));

    assertDoesNotThrow(() -> check.postProcessEnvironment(environment, null));

  }

}
