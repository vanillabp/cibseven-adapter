package io.vanillabp.cibseven.quarkus.migration;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The workflow aggregate of this module, a JPA entity on the application's datasource. Its
 * generated id is the business key of the workflow, on whichever engine it runs.
 */
@Entity
@Table(name = "MIGRATION_AGGREGATE")
@Getter
@Setter
public class MigrationAggregate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String content;

}
