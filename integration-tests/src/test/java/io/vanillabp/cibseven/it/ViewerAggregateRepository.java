package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewerAggregateRepository extends JpaRepository<ViewerTestAggregate, Long> {

}
