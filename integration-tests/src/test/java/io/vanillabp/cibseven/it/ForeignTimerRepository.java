package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ForeignTimerRepository extends JpaRepository<ForeignTimerAggregate, String> {

}
