package io.vanillabp.cibseven.it;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The test application. It boots a real embedded CIB seven engine on H2 together with the
 * VanillaBP CIB seven adapter. The engine shares H2 with the JPA aggregate persistence. Used by
 * {@link CibSevenStartWorkflowIT}.
 */
@SpringBootApplication
public class TestApplication {

}
