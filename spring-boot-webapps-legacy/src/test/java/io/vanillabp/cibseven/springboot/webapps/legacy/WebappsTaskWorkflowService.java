package io.vanillabp.cibseven.springboot.webapps.legacy;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import io.vanillabp.spi.process.ProcessService;
import io.vanillabp.spi.service.BpmnProcess;
import io.vanillabp.spi.service.TaskId;
import io.vanillabp.spi.service.WorkflowService;
import io.vanillabp.spi.service.WorkflowTask;

/**
 * Serves the process whose user task a web application completes. Only the tests which
 * complete a task switch the profile on.
 */
@Service
@Profile("webapps-task")
@WorkflowService(
    workflowAggregateClass = WebappsTaskAggregate.class,
    bpmnProcess = @BpmnProcess(bpmnProcessId = "WebappsTaskProcess"))
public class WebappsTaskWorkflowService {

  private final ProcessService<WebappsTaskAggregate> processService;

  public WebappsTaskWorkflowService(
      final ProcessService<WebappsTaskAggregate> processService) {

    this.processService = processService;

  }

  public void startWorkflow(
      final String id) {

    final var aggregate = new WebappsTaskAggregate();
    aggregate.setId(id);
    processService.startWorkflow(aggregate);

  }

  @WorkflowTask(id = "WT_Review")
  public void review(
      final WebappsTaskAggregate aggregate,
      @TaskId final String taskId) {

    aggregate.setReviewTaskId(taskId);

  }

  @WorkflowTask(id = "WT_Archive")
  public void archive(
      final WebappsTaskAggregate aggregate) {

    aggregate.setArchived(true);

  }

}
