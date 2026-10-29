package io.vanillabp.cibseven.processservice;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The core waits for a workflow to become findable only where the adapter asks for it.
 * CIB seven answers awareness probes from the same database and transaction which created
 * the instance. So it asks for nothing. A workflow this engine does not know does not
 * exist, and saying so has to stay immediate.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenWorkflowVisibilityTest {

  @Test
  @DisplayName("An embedded engine reports NO visibility delay - probes are answered from its own database")
  public void embeddedEngineReportsNoVisibilityDelay() {

    final var processService = new CibSevenProcessService<Object>(
        "cibseven", null, null, null, null, io.vanillabp.cibseven.TestCollaborators.complete());

    assertFalse(
        processService.workflowVisibilityDelay().isWaiting(),
        "CIB seven must not make the core wait for a workflow to show up");

  }

}
