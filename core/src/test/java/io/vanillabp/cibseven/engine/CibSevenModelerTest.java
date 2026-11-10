package io.vanillabp.cibseven.engine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.cibseven.bpm.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;

/**
 * The key <code>modeler-enabled</code>: CIB seven's modeler tables are off unless the
 * application switches them on, and a prefixed adapter id which switches them on is told at
 * boot that it needs the prefixed modeler table, too.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenModelerTest {

  private static final String[] ENGINE_MARKER_TABLES = {
      "ACT_RU_EXECUTION", "ACT_HI_PROCINST", "ACT_ID_USER", "ACT_RE_CASE_DEF", "ACT_HI_CASEINST", "ACT_RE_DECISION_DEF", "ACT_HI_DECINST"
  };

  private static DataSource h2WithTables(
      final String name,
      final String prefix,
      final String... tables) {

    final var dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1".formatted(name));
    try (var connection = dataSource.getConnection()) {
      for (final var table : tables) {
        try (Statement statement = connection.createStatement()) {
          statement.execute("create table %s%s (ID_ varchar(64))".formatted(prefix, table));
        }
      }
    } catch (final SQLException e) {
      throw new IllegalStateException(e);
    }
    return dataSource;

  }

  private static CibSevenEngineProperties prefixed(
      final boolean modelerEnabled) {

    final var properties = new CibSevenEngineProperties();
    properties.setTablePrefix("NEW_");
    properties.setDatabaseSchemaUpdate("false");
    properties.setModelerEnabled(modelerEnabled);
    return properties;

  }

  @Test
  @DisplayName("Without the key the engine leaves the modeler's tables alone, unlike CIB seven's default")
  public void theModelerIsOffByDefault() {

    final var configuration = new StandaloneInMemProcessEngineConfiguration();
    assertTrue(configuration.isModelerEnabled(), "CIB seven's own default changed");

    CibSevenModeler.applyTo("cs", configuration, new CibSevenEngineProperties());

    assertFalse(configuration.isModelerEnabled());

  }

  @Test
  @DisplayName("The key switches the modeler's tables on")
  public void theKeySwitchesTheModelerOn() {

    final var configuration = new StandaloneInMemProcessEngineConfiguration();
    final var properties = new CibSevenEngineProperties();
    properties.setModelerEnabled(true);

    CibSevenModeler.applyTo("cs", configuration, properties);

    assertTrue(configuration.isModelerEnabled());

  }

  @Test
  @DisplayName("A prefixed id without the modeler needs only the tables a Camunda 7 engine knows")
  public void aPrefixWithoutTheModelerNeedsNoModelerTable() {

    final var dataSource = h2WithTables("modeler-off", "NEW_", ENGINE_MARKER_TABLES);

    assertDoesNotThrow(() -> CibSevenTablePrefixSchema.validate("cs", prefixed(false), dataSource));

  }

  @Test
  @DisplayName("A prefixed id with the modeler is told at boot that the prefixed modeler table is missing")
  public void aPrefixWithTheModelerNeedsTheModelerTable() {

    final var dataSource = h2WithTables("modeler-on", "NEW_", ENGINE_MARKER_TABLES);

    final var exception = assertThrows(
        IllegalStateException.class,
        () -> CibSevenTablePrefixSchema.validate("cs", prefixed(true), dataSource));

    final var message = exception.getMessage();
    assertTrue(message.contains("NEW_MOD_ELEMENT_TEMPLATES"), () -> message);
    assertFalse(message.contains("NEW_ACT_RU_EXECUTION"), () -> message);

  }

  @Test
  @DisplayName("A prefixed id with the modeler passes where the prefixed modeler table is there")
  public void aPrefixWithTheModelerAndItsTablePasses() {

    final var dataSource = h2WithTables("modeler-on-complete", "NEW_", ENGINE_MARKER_TABLES);
    try (var connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
      statement.execute("create table NEW_MOD_ELEMENT_TEMPLATES (ID_ varchar(64))");
    } catch (final SQLException e) {
      throw new IllegalStateException(e);
    }

    assertDoesNotThrow(() -> CibSevenTablePrefixSchema.validate("cs", prefixed(true), dataSource));

  }

}
