package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AsyncByElementIdRepository extends JpaRepository<AsyncByElementIdAggregate, Long> {
}
