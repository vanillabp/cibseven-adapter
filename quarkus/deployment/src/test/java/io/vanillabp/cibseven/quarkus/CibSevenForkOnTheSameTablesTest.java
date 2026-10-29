package io.vanillabp.cibseven.quarkus;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.vanillabp.cibseven.quarkus.fork.OperatonAdapterDouble;
import io.vanillabp.cibseven.quarkus.sample.TestAggregate;
import io.vanillabp.cibseven.quarkus.sample.TestAggregatePersistence;
import io.vanillabp.cibseven.quarkus.sample.TestWorkflowService;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * A fork of Camunda 7 uses the same tables. A <code>cibseven</code> id and an id of a fork
 * adapter on the same datasource without prefixes would be two engines on one set of tables,
 * and each job executor would run the jobs of the other one. The check of one adapter type
 * cannot see that, so the platform compares the tables of every adapter id across types. The
 * tables of <code>c7</code> come from the Quarkus configuration, the ones of the fork from a
 * double.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenForkOnTheSameTablesTest {

  @RegisterExtension
  static final QuarkusExtensionTest extensionTest = new QuarkusExtensionTest()
      .setArchiveProducer(() -> ShrinkWrap
          .create(JavaArchive.class)
          .addClass(TestAggregate.class)
          .addClass(TestAggregatePersistence.class)
          .addClass(TestWorkflowService.class)
          .addClass(OperatonAdapterDouble.class)
          .addAsResource("fork-same-tables/application.yaml", "application.yaml")
          .addAsResource("c7-test/processes/test-process.bpmn", "c7-test/processes/test-process.bpmn")
          .addAsResource("workflow-module-descriptor/workflow-module", "META-INF/workflow-module"))
      .assertException(throwable -> {
        final var messages = CibSevenBootFailure.messagesOf(throwable);
        Assertions.assertTrue(
            messages.contains("The adapters 'c7' (type 'cibseven'), 'cib' (type 'operaton') work on the SAME "
                + "system: datasource <the application's default datasource>, table prefix <no table prefix>."),
            () -> messages);
        Assertions.assertTrue(messages.contains("for type 'cibseven', 'operaton':"), () -> messages);
        Assertions.assertTrue(messages.contains("'vanillabp.adapters.<id>.data-source-name'"), () -> messages);
        Assertions.assertTrue(messages.contains("'vanillabp.adapters.<id>.table-prefix'"), () -> messages);
      });

  @Test
  public void aCibSevenIdAndAForkIdOnTheSameTablesFailTheBoot() {
    // the assertion happens on the startup exception (assertException above)
  }

}
