package io.vanillabp.cibseven.wiring;

import org.cibseven.bpm.engine.impl.el.JuelExpressionManager;
import org.cibseven.bpm.impl.juel.jakarta.el.CompositeELResolver;
import org.cibseven.bpm.impl.juel.jakarta.el.ELResolver;

import io.vanillabp.integration.adapter.spi.workflowtask.WorkflowTaskInvoker;

/**
 * The engine's default (JUEL) expression manager, extended by VanillaBP's
 * {@link CibSevenTaskELResolver}. It is used on platforms without a bean-aware
 * expression manager, which is Quarkus. The Spring Boot module extends Camunda's
 * {@code SpringExpressionManager} the same way, so Spring beans stay resolvable in EL.
 */
public class CibSevenTaskExpressionManager extends JuelExpressionManager {

  private final CibSevenTaskRegistry taskRegistry;

  private final WorkflowTaskInvoker workflowTaskInvoker;

  /**
   * Adds this adapter's resolver behind the engine's own ones. So a process variable answers
   * a name before a task does (see decision 46 in the Camunda 7 adapter's {@code DECISIONS.md}). It is for a
   * platform which has no bean-aware expression manager to extend instead.
   *
   * @param taskRegistry What the resolver built here looks a task up in
   * @param workflowTaskInvoker Where the application's method is called
   */
  public CibSevenTaskExpressionManager(
      final CibSevenTaskRegistry taskRegistry,
      final WorkflowTaskInvoker workflowTaskInvoker) {

    this.taskRegistry = taskRegistry;
    this.workflowTaskInvoker = workflowTaskInvoker;

  }

  @Override
  protected ELResolver createElResolver() {

    final var compositeResolver = (CompositeELResolver) super.createElResolver();
    compositeResolver.add(new CibSevenTaskELResolver(taskRegistry, workflowTaskInvoker));
    return compositeResolver;

  }

}
