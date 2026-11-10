package io.vanillabp.cibseven.quarkus.plugins;

import org.cibseven.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.cibseven.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * An engine plugin of the application, configured under {@code engine-plugins}, which
 * changes a setting the adapter leaves alone.
 */
public class RetriesPlugin extends AbstractProcessEnginePlugin {

  @Override
  public void preInit(
      final ProcessEngineConfigurationImpl configuration) {
    configuration.setDefaultNumberOfRetries(5);
  }

}
