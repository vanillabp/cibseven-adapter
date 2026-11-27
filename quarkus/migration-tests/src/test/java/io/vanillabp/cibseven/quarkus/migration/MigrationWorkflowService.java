package io.vanillabp.cibseven.quarkus.migration;

import io.vanillabp.spi.process.ProcessService;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.WorkflowService;
import io.vanillabp.spi.service.WorkflowTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * The workflow service of the process <code>MigrationProcess</code>. The same service serves
 * the process on both engines: each adapter delivers the tasks of the workflows which run on
 * its own engine.
 */
@ApplicationScoped
@WorkflowService(
    workflowAggregateClass = MigrationAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "MigrationProcess"))
public class MigrationWorkflowService {

  @Inject
  ProcessService<MigrationAggregate> processService;

  /**
   * Starts a workflow the way the application does, on the adapter listed first.
   *
   * @param content What the aggregate holds before its task ran
   * @return The saved aggregate
   */
  public MigrationAggregate startWorkflow(
      final String content) {

    final var aggregate = new MigrationAggregate();
    aggregate.setContent(content);
    return processService.startWorkflow(aggregate);

  }

  /**
   * The one task of the process. It marks the aggregate, so a test sees that it was delivered.
   *
   * @param aggregate The workflow aggregate
   */
  @WorkflowTask
  public void markDone(
      final MigrationAggregate aggregate) {

    aggregate.setContent("task-done");

  }

}
