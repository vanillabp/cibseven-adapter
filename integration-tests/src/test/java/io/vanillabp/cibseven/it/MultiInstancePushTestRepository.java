package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MultiInstancePushTestRepository extends JpaRepository<MultiInstancePushTestAggregate, Long> {

}
