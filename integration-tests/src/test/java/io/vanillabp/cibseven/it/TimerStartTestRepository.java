package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TimerStartTestRepository extends JpaRepository<TimerStartTestAggregate, String> {

}
