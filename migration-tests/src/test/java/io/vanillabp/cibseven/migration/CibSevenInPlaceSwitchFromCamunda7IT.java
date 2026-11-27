package io.vanillabp.cibseven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.camunda7.springboot.engine.Camunda7EngineHolder;
import io.vanillabp.cibseven.springboot.engine.CibSevenEngineHolder;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * An application which stops on the Camunda 7 adapter and starts again on this one, on the same
 * database and with the same adapter id. CIB seven runs on the tables of Camunda 7.24. See
 * decision 7 in the repository's DECISIONS.md.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class CibSevenInPlaceSwitchFromCamunda7IT {

  private static final String DATABASE = "in-place-switch";

  @Test
  @DisplayName("A workflow started on Camunda 7 finishes on CIB seven after the switch")
  public void aRunningWorkflowFinishesAfterTheSwitch() {

    final var camunda7 = new HashMap<String, Object>(MigrationContexts.adapter("engine", "camunda7"));
    try (var before = MigrationContexts.boot(DATABASE, camunda7)) {
      final var aggregate = new MigrationAggregate();
      aggregate.setId("switched");
      aggregate.setAmount(1000);
      before.getBean(MigrationWorkflowService.class).start(aggregate);
      Await.until(() -> MigrationWorkflowService.WAITING_USER_TASKS.get("switched"), "the user task on Camunda 7");
      assertEquals(
          1,
          before
              .getBean("Camunda7_Engine_engine", Camunda7EngineHolder.class)
              .getRuntimeService()
              .createProcessInstanceQuery()
              .processInstanceBusinessKey("switched")
              .count());
    }

    final var cibSeven = new HashMap<String, Object>(MigrationContexts.adapter("engine", "cibseven"));
    try (var after = MigrationContexts.boot(DATABASE, cibSeven)) {
      final var aggregate = after.getBean(MigrationRepository.class).findById("switched").orElseThrow();
      after
          .getBean(MigrationWorkflowService.class)
          .approve(aggregate, MigrationWorkflowService.WAITING_USER_TASKS.get("switched"));

      final var engine = after.getBean("CibSeven_Engine_engine", CibSevenEngineHolder.class);
      final var finished = Await.until(
          () -> engine
              .getHistoryService()
              .createHistoricProcessInstanceQuery()
              .processInstanceBusinessKey("switched")
              .finished()
              .singleResult(),
          "the workflow to finish on CIB seven");
      Await.until(
          () -> after.getBean(MigrationRepository.class).findById("switched").filter(MigrationAggregate::isFinished)
              .orElse(null),
          "the last task, which only CIB seven can have delivered");

      final var repositoryService = engine.getRepositoryService();
      // both adapters write the source '<adapter id>:<workflow module id>', and the model
      // is the same. So the engine's duplicate filter finds the deployment of the Camunda 7
      // adapter, and the first start on CIB seven deploys nothing new
      assertEquals(
          List.of("engine:migration"),
          repositoryService.createDeploymentQuery().list().stream().map(deployment -> deployment.getSource())
              .toList(),
          "the deployment of the Camunda 7 adapter is the one of this adapter, too");
      assertEquals(
          1,
          repositoryService.createProcessDefinitionQuery().processDefinitionKey("MigrationProcess").count(),
          "so the process keeps its version");
      assertEquals(
          1,
          repositoryService.getProcessDefinition(finished.getProcessDefinitionId()).getVersion(),
          "and the running workflow finished on the version it started on");
    }

  }

}
