package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ForeignSignalRepository extends JpaRepository<ForeignSignalAggregate, String> {

}
