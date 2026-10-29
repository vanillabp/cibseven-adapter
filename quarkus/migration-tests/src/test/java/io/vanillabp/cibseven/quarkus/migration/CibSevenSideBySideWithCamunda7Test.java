package io.vanillabp.cibseven.quarkus.migration;

import java.util.function.Supplier;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.camunda7.quarkus.runtime.Camunda7QuarkusEngineRegistry;
import io.vanillabp.cibseven.quarkus.runtime.CibSevenQuarkusEngineRegistry;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;

/**
 * The migration from Camunda 7 on Quarkus: the Camunda 7 adapter and this adapter in one
 * application, each with an engine of its own. Camunda 7 runs on the application's
 * datasource, CIB seven on a datasource of its own, and CIB seven is listed first. See
 * decision 4 in the repository's DECISIONS.md.
 * <p>
 * A workflow which ran on Camunda 7 before the migration goes on there, a new workflow starts
 * on CIB seven, and each engine's job executor runs only the jobs of its own engine.
 * <p>
 * Both datasources are XA datasources. A task of the engine on the second datasource loads
 * the aggregate from the application's datasource in the same transaction, and so does the
 * outbox which starts a workflow there. Without XA the transaction refuses the second
 * datasource: the task handler runs, but the job of the engine rolls back.
 */
@ExtendWith(SuppressOutputExtension.class)
@SuppressOutputExtension.SuppressBackgroundOutput
public class CibSevenSideBySideWithCamunda7Test {

  private static final String MODULE_ID = "migration";

  private static final String BPMN_PROCESS_ID = "MigrationProcess";

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .setArchiveProducer(() -> ShrinkWrap
          .create(JavaArchive.class)
          .addClass(MigrationAggregate.class)
          .addClass(MigrationAggregatePersistence.class)
          .addClass(MigrationWorkflowService.class)
          .addAsResource("side-by-side/application.yaml", "application.yaml")
          .addAsResource("migration/processes/migration-process.bpmn", "migration/processes/migration-process.bpmn")
          .addAsResource("workflow-module-descriptor/workflow-module", "META-INF/workflow-module"));

  @Inject
  Camunda7QuarkusEngineRegistry camunda7Engines;

  @Inject
  CibSevenQuarkusEngineRegistry cibSevenEngines;

  @Inject
  MigrationWorkflowService workflowService;

  @Inject
  EntityManager entityManager;

  @Inject
  UserTransaction userTransaction;

  @Test
  public void bothEnginesServeTheirWorkflowsInOneApplication() throws Exception {

    final var camunda7 = camunda7Engines.engineFor("c7");
    final var cibSeven = cibSevenEngines.engineFor("cs");
    Assertions.assertEquals("vanillabp-camunda7-c7", camunda7.getProcessEngine().getName());
    Assertions.assertEquals("vanillabp-cibseven-cs", cibSeven.getProcessEngine().getName());

    // a workflow which ran on Camunda 7 before the migration. The application started it
    // there while Camunda 7 was listed first, so here it is started in that engine directly
    final var runningBefore = inTransaction(() -> {
      final var aggregate = new MigrationAggregate();
      aggregate.setContent("running-before");
      entityManager.persist(aggregate);
      entityManager.flush();
      return aggregate.getId();
    });
    camunda7
        .getRuntimeService()
        .createProcessInstanceByKey(BPMN_PROCESS_ID)
        .processDefinitionTenantId(MODULE_ID)
        .businessKey(String.valueOf(runningBefore))
        .execute();

    // a new workflow, started the way the application starts it: on the adapter listed
    // first, which is CIB seven now
    final var startedAfter = inTransaction(() -> workflowService
        .startWorkflow("started-after")
        .getId());

    awaitTaskDone(runningBefore);
    awaitTaskDone(startedAfter);

    // the adapters end each task asynchronously, so the workflows finish a moment after
    // their tasks ran
    final var camunda7History = camunda7.getProcessEngine().getHistoryService();
    final var cibSevenHistory = cibSeven.getProcessEngine().getHistoryService();
    awaitTrue(() -> camunda7History.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey(String.valueOf(runningBefore)).finished().count() == 1,
        "the workflow started before the migration to finish on Camunda 7");
    awaitTrue(() -> cibSevenHistory.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey(String.valueOf(startedAfter)).finished().count() == 1,
        "the workflow started after the migration to finish on CIB seven");
    Assertions.assertEquals(0, cibSevenHistory.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey(String.valueOf(runningBefore)).count(),
        "CIB seven's job executor must not take the jobs of Camunda 7");
    Assertions.assertEquals(0, camunda7History.createHistoricProcessInstanceQuery()
        .processInstanceBusinessKey(String.valueOf(startedAfter)).count(),
        "the new workflow belongs to the adapter listed first");

  }

  private static void awaitTrue(
      final java.util.function.BooleanSupplier condition,
      final String what) throws InterruptedException {

    final var deadline = System.currentTimeMillis() + 30_000;
    while (!condition.getAsBoolean()) {
      Assertions.assertTrue(System.currentTimeMillis() < deadline, () -> "waited 30s for "
          + what);
      Thread.sleep(100);
    }

  }

  private void awaitTaskDone(
      final Long aggregateId) throws Exception {

    final var deadline = System.currentTimeMillis() + 30_000;
    while (!"task-done".equals(inTransaction(() -> {
      entityManager.clear();
      return entityManager
          .find(MigrationAggregate.class, aggregateId)
          .getContent();
    }))) {
      Assertions.assertTrue(
          System.currentTimeMillis() < deadline,
          () -> "the task of workflow "
              + aggregateId
              + " did not run within 30s");
      Thread.sleep(100);
    }

  }

  private <T> T inTransaction(
      final Supplier<T> work) throws Exception {

    userTransaction.begin();
    try {
      final var result = work.get();
      userTransaction.commit();
      return result;
    } catch (final RuntimeException e) {
      userTransaction.rollback();
      throw e;
    }

  }

}
