package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MiUserTaskRepository extends JpaRepository<MiUserTaskAggregate, Long> {

}
