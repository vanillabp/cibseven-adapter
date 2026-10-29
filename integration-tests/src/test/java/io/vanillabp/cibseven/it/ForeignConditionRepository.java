package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ForeignConditionRepository extends JpaRepository<ForeignConditionAggregate, String> {

}
