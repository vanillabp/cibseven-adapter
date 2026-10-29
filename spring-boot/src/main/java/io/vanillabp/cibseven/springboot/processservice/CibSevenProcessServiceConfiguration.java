package io.vanillabp.cibseven.springboot.processservice;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.vanillabp.cibseven.springboot.CibSevenAdapterBeanRegistrar;
import io.vanillabp.cibseven.springboot.VanillaBpCibSevenProperties;

/**
 * Wires the beans of the CIB seven adapter per adapter id. The imported
 * {@link CibSevenAdapterBeanRegistrar} registers them. For each configured adapter id of
 * type {@code cibseven} there is ONE embedded engine ({@code CibSevenEngineHolder}), ONE
 * process-service <i>element</i> bean and ONE deployment-service <i>element</i> bean.
 * Without a configured {@code cibseven} adapter id no bean is registered. The registrar
 * reads the set of ids from the configuration. So no {@code @Conditional} is needed here.
 * The order relative to the datasource and transaction-manager auto-configurations does
 * not matter either. The engine holders resolve the application's {@code DataSource} and
 * {@code PlatformTransactionManager} LAZILY, when the bean is created (context refresh).
 * If none is available, they fail with a guiding message.
 */
@AutoConfiguration
@EnableConfigurationProperties(VanillaBpCibSevenProperties.class)
@Import(CibSevenAdapterBeanRegistrar.class)
public class CibSevenProcessServiceConfiguration {

  /**
   * Spring Boot builds the class to apply the annotations above. It declares no bean
   * method of its own. The beans of this adapter are registered per configured adapter id,
   * and only the registrar can do that.
   */
  public CibSevenProcessServiceConfiguration() {

  }

}
