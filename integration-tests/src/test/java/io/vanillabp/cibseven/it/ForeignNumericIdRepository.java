package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ForeignNumericIdRepository extends JpaRepository<ForeignNumericIdAggregate, Long> {

}
