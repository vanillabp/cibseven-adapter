package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AggregateChangedTestRepository extends JpaRepository<AggregateChangedTestAggregate, Long> {

}
