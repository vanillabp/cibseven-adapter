package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedNameRepository extends JpaRepository<SharedNameAggregate, String> {
}
