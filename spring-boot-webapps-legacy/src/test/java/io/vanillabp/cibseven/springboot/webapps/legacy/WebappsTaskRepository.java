package io.vanillabp.cibseven.springboot.webapps.legacy;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * The store of {@link WebappsTaskAggregate}.
 */
public interface WebappsTaskRepository extends JpaRepository<WebappsTaskAggregate, String> {
}
