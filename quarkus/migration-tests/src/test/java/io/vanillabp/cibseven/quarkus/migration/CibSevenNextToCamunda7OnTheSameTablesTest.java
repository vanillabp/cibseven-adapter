package io.vanillabp.cibseven.quarkus.migration;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A <code>camunda7</code> id and a <code>cibseven</code> id on the application's datasource,
 * without a table prefix, would be two engines on one set of tables. Each job executor would
 * run the jobs of the other engine. The start refuses it, with the real Camunda 7 adapter next
 * to this one, through the check across adapter types (decision 47 in the Camunda 7 adapter's
 * DECISIONS.md).
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenNextToCamunda7OnTheSameTablesTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .setArchiveProducer(() -> ShrinkWrap
          .create(JavaArchive.class)
          .addClass(MigrationAggregate.class)
          .addClass(MigrationAggregatePersistence.class)
          .addClass(MigrationWorkflowService.class)
          .addAsResource("same-tables/application.yaml", "application.yaml")
          .addAsResource("migration/processes/migration-process.bpmn", "migration/processes/migration-process.bpmn")
          .addAsResource("workflow-module-descriptor/workflow-module", "META-INF/workflow-module"))
      .assertException(throwable -> {
        final var messages = BootFailure.messagesOf(throwable);
        Assertions.assertTrue(messages.contains("'c7' (type 'camunda7')"), () -> messages);
        Assertions.assertTrue(messages.contains("'cs' (type 'cibseven')"), () -> messages);
        Assertions.assertTrue(messages.contains("work on the SAME system"), () -> messages);
        Assertions.assertTrue(messages.contains("'vanillabp.adapters.<id>.data-source-name'"), () -> messages);
      });

  @Test
  public void bothAdaptersOnTheSameTablesFailTheBoot() {
    // the assertion happens on the startup exception (assertException above)
  }

}
