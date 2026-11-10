package io.vanillabp.cibseven.quarkus.migration;

import io.vanillabp.integration.spi.AggregatePersistenceAware;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Saves and loads the {@link MigrationAggregate} in the caller's JTA transaction.
 */
@ApplicationScoped
public class MigrationAggregatePersistence implements AggregatePersistenceAware<MigrationAggregate> {

  @Inject
  EntityManager entityManager;

  @Override
  public Class<MigrationAggregate> getAggregateClass() {

    return MigrationAggregate.class;

  }

  @Override
  public MigrationAggregate save(
      final MigrationAggregate aggregate) {

    if (aggregate.getId() == null) {
      entityManager.persist(aggregate);
      // the generated id is the business key of the workflow
      entityManager.flush();
      return aggregate;
    }
    return entityManager.merge(aggregate);

  }

  @Override
  public Object getAggregateId(
      final MigrationAggregate aggregate) {

    return aggregate.getId();

  }

  @Override
  public MigrationAggregate loadById(
      final Object aggregateId) {

    return entityManager.find(MigrationAggregate.class, aggregateId);

  }

}
