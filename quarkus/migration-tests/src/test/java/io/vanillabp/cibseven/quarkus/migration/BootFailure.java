package io.vanillabp.cibseven.quarkus.migration;

/**
 * What a boot which refused said, as one string. Which layer wraps a refusal differs per
 * check, so a test reads the whole chain and not one cause of it.
 */
final class BootFailure {

  private BootFailure() {
  }

  /**
   * @param throwable What the boot threw
   * @return The messages of the whole chain, one per line
   */
  static String messagesOf(
      final Throwable throwable) {

    final var messages = new StringBuilder();
    var cause = throwable;
    while (cause != null) {
      messages
          .append(cause.getMessage())
          .append('\n');
      cause = cause.getCause() == cause
          ? null
          : cause.getCause();
    }
    return messages.toString();

  }

}
