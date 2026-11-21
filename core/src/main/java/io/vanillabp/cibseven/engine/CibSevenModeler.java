package io.vanillabp.cibseven.engine;

import org.cibseven.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Whether the engine runs the tables of CIB seven's modeler, for both platforms.
 * <p>
 * CIB seven 2.2 added a modeler and an AI chat to the engine, with tables of their own: the
 * <code>MOD_*</code> tables and <code>CHAT_MESSAGES</code>. CIB seven 3.0 calls them the
 * webclient extensions and adds a few more tables. While the engine setting
 * {@code webclientExtensionsEnabled} is <code>true</code>, the engine creates these tables, and
 * with <code>database-schema-update: false</code> it checks that they are there. The check
 * looks for <code>MOD_ELEMENT_TEMPLATES</code> under the table prefix. So an adapter id with a
 * <code>table-prefix</code> would not start on a schema which holds only the tables a Camunda 7
 * engine knows. The engine says <code>ENGINE-03056 Tables are missing for the following
 * components</code>. In CIB seven 2.2 the setting was called {@code modelerEnabled} and was
 * <code>true</code> by default. Since 3.0 it is <code>false</code> by default.
 * <p>
 * VanillaBP does not use the modeler. So the key
 * <code>vanillabp.adapters.&lt;id&gt;.modeler-enabled</code> is <code>false</code> unless the
 * application sets it, and the engine neither creates nor checks the modeler tables. An
 * application which wants the modeler sets it to <code>true</code>. With a table prefix it
 * then needs the prefixed modeler tables as well, and {@link CibSevenTablePrefixSchema} checks
 * them while the application boots. Decision 5 in the repository's DECISIONS.md says why.
 */
public final class CibSevenModeler {

  private static final Logger log = LoggerFactory.getLogger(CibSevenModeler.class);

  /**
   * The table the engine asks for to find out whether the modeler's tables are there. It is
   * the one {@code DbSqlSession#isModelerTablePresent} names.
   */
  static final String MODELER_TABLE = "MOD_ELEMENT_TEMPLATES";

  private CibSevenModeler() {
    // static helper
  }

  /**
   * Applies the key to an engine which is about to be built. It is called for every adapter
   * id, so the engine's own default never decides.
   *
   * @param adapterId The adapter id whose engine is being built
   * @param configuration The engine configuration, before the engine is built
   * @param properties The adapter id's engine settings
   */
  public static void applyTo(
      final String adapterId,
      final ProcessEngineConfigurationImpl configuration,
      final CibSevenEngineProperties properties) {

    configuration.setWebclientExtensionsEnabled(properties.isModelerEnabled());
    if (properties.isModelerEnabled()) {
      log
          .info(
              """
                  CibSeven[{}]: the engine runs the tables of CIB seven's modeler \
                  ('vanillabp.adapters.{}.modeler-enabled: true'). VanillaBP does not use them.""",
              adapterId,
              adapterId);
    }

  }

}
