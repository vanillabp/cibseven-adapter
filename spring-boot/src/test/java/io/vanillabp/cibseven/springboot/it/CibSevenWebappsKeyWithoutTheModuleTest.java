package io.vanillabp.cibseven.springboot.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.vanillabp.cibseven.springboot.CibSevenAdapterConfiguration;
import io.vanillabp.cibseven.springboot.processservice.CibSevenProcessServiceConfiguration;
import io.vanillabp.integration.processservice.SpringBootMigrationAdapterAutoConfiguration;
import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.integration.workflowmodule.WorkflowModuleAutoConfiguration;

/**
 * <code>vanillabp.webapps.ui</code> chooses the web applications. The module
 * <code>cibseven-adapter-spring-boot-webapps</code> reads it. Without that module nothing reads
 * the key, so it would change nothing, and the start ends like for any key nobody reads.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenWebappsKeyWithoutTheModuleTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withInitializer(new ConfigDataApplicationContextInitializer())
      .withConfiguration(
          AutoConfigurations.of(
              org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration.class,
              org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration.class,
              CibSevenAdapterConfiguration.class,
              CibSevenProcessServiceConfiguration.class,
              WorkflowModuleAutoConfiguration.class,
              SpringBootMigrationAdapterAutoConfiguration.class));

  @Test
  @DisplayName("vanillabp.webapps.ui without the webapps module ends the start")
  public void theKeyEndsTheStart() {

    this.contextRunner
        .withPropertyValues(
            "spring.config.location=classpath:application.yaml",
            "spring.datasource.url=jdbc:h2:mem:c7-webapps-key-without-module;DB_CLOSE_DELAY=-1",
            "vanillabp.webapps.ui=webclient")
        .run(context -> {

          Assertions.assertNotNull(context.getStartupFailure(), "the start has to end");
          final var causes = new StringBuilder();
          for (Throwable cause = context.getStartupFailure(); cause != null; cause = cause.getCause()) {
            causes
                .append(cause.getMessage())
                .append('\n');
          }
          final var message = causes.toString();
          Assertions.assertTrue(message.contains("Nothing reads this key below 'vanillabp'"), message);
          Assertions.assertTrue(message.contains("webapps"), message);

        });

  }

}
