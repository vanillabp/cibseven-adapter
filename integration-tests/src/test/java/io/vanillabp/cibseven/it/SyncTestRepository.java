package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncTestRepository extends JpaRepository<SyncTestAggregate, Long> {
}
