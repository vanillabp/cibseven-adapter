package io.vanillabp.cibseven.it;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallByExpressionRepository extends JpaRepository<CallByExpressionAggregate, Long> {

}
