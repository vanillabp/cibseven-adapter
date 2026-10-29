package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RenamedProcessRepository extends JpaRepository<RenamedProcessAggregate, Long> {

}
