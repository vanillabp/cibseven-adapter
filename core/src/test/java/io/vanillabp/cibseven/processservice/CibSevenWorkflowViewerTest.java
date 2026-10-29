package io.vanillabp.cibseven.processservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.cibseven.bpm.engine.HistoryService;
import org.cibseven.bpm.engine.ProcessEngineException;
import org.cibseven.bpm.engine.RepositoryService;
import org.cibseven.bpm.engine.RuntimeService;
import org.cibseven.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vanillabp.integration.test.utils.SuppressOutputExtension;
import io.vanillabp.spi.process.WorkflowElementType;

/**
 * Unit tests of the degraded paths of the CIB seven viewer and history API. A real engine
 * does not reach these paths in the integration tests: an engine running with history level
 * {@code none}, a workflow unknown to this engine, a history context of a FOREIGN workflow,
 * and the activity-type mapping.
 */
@ExtendWith(SuppressOutputExtension.class)
public class CibSevenWorkflowViewerTest {

  private final RepositoryService repositoryService = mock(RepositoryService.class, RETURNS_DEEP_STUBS);

  private final HistoryService historyService = mock(HistoryService.class, RETURNS_DEEP_STUBS);

  private final RuntimeService runtimeService = mock(RuntimeService.class, RETURNS_DEEP_STUBS);

  /**
   * The engine's query builders are fluent AND generic. {@code RETURNS_SELF} makes every
   * filter call return the mock itself. So only the terminal
   * {@code list()}/{@code singleResult()} needs stubbing. Deep stubs do not work here,
   * because their type inference fails on the generic {@code Query} methods.
   */
  private final org.cibseven.bpm.engine.history.HistoricProcessInstanceQuery historicInstanceQuery = mock(
      org.cibseven.bpm.engine.history.HistoricProcessInstanceQuery.class, RETURNS_SELF);

  private final org.cibseven.bpm.engine.runtime.ProcessInstanceQuery instanceQuery = mock(
      org.cibseven.bpm.engine.runtime.ProcessInstanceQuery.class, RETURNS_SELF);

  private final org.cibseven.bpm.engine.repository.ProcessDefinitionQuery definitionQuery = mock(
      org.cibseven.bpm.engine.repository.ProcessDefinitionQuery.class, RETURNS_SELF);

  /**
   * What the call activities of an instance called. Nothing, unless a test says otherwise.
   */
  private final org.cibseven.bpm.engine.history.HistoricActivityInstanceQuery activityQuery = mock(
      org.cibseven.bpm.engine.history.HistoricActivityInstanceQuery.class, RETURNS_SELF);

  private final CibSevenWorkflowViewer viewer = new CibSevenWorkflowViewer(
      "c7", repositoryService, historyService, runtimeService);

  @org.junit.jupiter.api.BeforeEach
  public void wireQueries() {

    when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicInstanceQuery);
    when(runtimeService.createProcessInstanceQuery()).thenReturn(instanceQuery);
    when(repositoryService.createProcessDefinitionQuery()).thenReturn(definitionQuery);
    when(historyService.createHistoricActivityInstanceQuery()).thenReturn(activityQuery);
    when(activityQuery.list()).thenReturn(List.of());

  }

  /**
   * No historic instance for the aggregate. Either the engine runs with history level
   * {@code none}, or the history was cleaned up.
   */
  private void withoutHistory() {

    when(historicInstanceQuery.list()).thenReturn(List.of());

  }

  @Test
  @DisplayName("A workflow module without a tenant asks without one - Camunda rejects a null tenant id")
  public void aModuleWithoutTenantIsQueriedWithoutTenant() {

    // a workflow module reached with 'name-clash-avoidance: none' (or 'use-prefix') has no
    // tenant at all. Passing null to tenantIdIn does not mean "any tenant". The engine
    // throws NullValueException('tenantIds contains null value'). That used to make every
    // viewer call of a default-configured application fail.
    withoutHistory();
    when(instanceQuery.singleResult()).thenReturn(null);

    assertEquals(List.of(), viewer.getProcessDefinitions("module", "Process", null, "42", null));
    assertNull(viewer.getWorkflowHistory("module", "Process", null, "42", null));

    org.mockito.Mockito.verify(instanceQuery, org.mockito.Mockito.atLeastOnce()).withoutTenantId();
    org.mockito.Mockito.verify(historicInstanceQuery, org.mockito.Mockito.atLeastOnce()).withoutTenantId();
    org.mockito.Mockito
        .verify(instanceQuery, org.mockito.Mockito.never())
        .tenantIdIn(org.mockito.ArgumentMatchers.any());
    org.mockito.Mockito
        .verify(historicInstanceQuery, org.mockito.Mockito.never())
        .tenantIdIn(org.mockito.ArgumentMatchers.any());

  }

  @Test
  @DisplayName("A workflow unknown to this engine is reported as unknown (empty list / null history)")
  public void unknownWorkflowIsReportedAsUnknown() {

    withoutHistory();
    when(instanceQuery.singleResult()).thenReturn(null);

    assertEquals(List.of(), viewer.getProcessDefinitions("module", "Process", "module", "42", null));
    assertNull(viewer.getWorkflowHistory("module", "Process", "module", "42", null));

  }

  @Test
  @DisplayName("Without history the running instance still answers the definition - with a null element history")
  public void withoutHistoryTheRunningInstanceAnswers() {

    withoutHistory();
    final var instance = mock(ProcessInstance.class);
    when(instance.getProcessDefinitionId()).thenReturn("Process:1:aaa");
    when(instanceQuery.singleResult()).thenReturn(instance);
    when(repositoryService
        .getProcessDefinition("Process:1:aaa")
        .getId())
        .thenReturn("Process:1:aaa");
    when(repositoryService
        .getProcessDefinition("Process:1:aaa")
        .getKey())
        .thenReturn("Process");
    when(repositoryService
        .getProcessDefinition("Process:1:aaa")
        .getVersion())
        .thenReturn(1);
    when(repositoryService
        .getBpmnModelInstance("Process:1:aaa")
        .getModelElementsByType(org.cibseven.bpm.model.bpmn.instance.CallActivity.class))
        .thenReturn(List.of());

    final var definitions = viewer.getProcessDefinitions("module", "Process", "module", "42", null);
    assertEquals(1, definitions.size());
    assertEquals("Process:1:aaa", definitions
        .getFirst()
        .id());

    final var history = viewer.getWorkflowHistory("module", "Process", "module", "42", null);
    assertEquals("Process:1:aaa", history.processDefinitionId());
    assertNull(history.startTime());
    assertNull(
        history.elementsHistory(),
        "history level 'none' means no element history - the SPI expresses that as null");

  }

  @Test
  @DisplayName("A history context of a foreign workflow is rejected")
  public void foreignHistoryContextIsRejected() {

    final var primaryInstance = mock(org.cibseven.bpm.engine.history.HistoricProcessInstance.class);
    when(primaryInstance.getId()).thenReturn("primary-instance");
    when(historicInstanceQuery.list()).thenReturn(List.of(primaryInstance));

    final var foreignInstance = mock(org.cibseven.bpm.engine.history.HistoricProcessInstance.class);
    when(foreignInstance.getRootProcessInstanceId()).thenReturn("another-workflows-instance");
    when(historicInstanceQuery.singleResult()).thenReturn(foreignInstance);
    when(instanceQuery.singleResult()).thenReturn(null);

    assertEquals(List.of(), viewer.getProcessDefinitions("module", "Process", "module", "42", "foreign-context"));
    assertNull(viewer.getWorkflowHistory("module", "Process", "module", "42", "foreign-context"));

  }

  @Test
  @DisplayName("An unknown process definition has no BPMN XML")
  public void unknownProcessDefinitionHasNoXml() {

    when(repositoryService.getProcessModel("does-not-exist"))
        .thenThrow(new ProcessEngineException("no such definition"));

    assertNull(viewer.getBpmnXml("does-not-exist"));

  }

  @Test
  @DisplayName("CIB seven activity types map onto the SPI's element types")
  public void activityTypesAreMapped() {

    assertEquals(WorkflowElementType.UNKNOWN, CibSevenWorkflowViewer.elementTypeOf(null));
    assertEquals(WorkflowElementType.UNKNOWN, CibSevenWorkflowViewer.elementTypeOf("somethingNew"));
    // events. Camunda names them in fine detail, the SPI groups them
    assertEquals(WorkflowElementType.START_EVENT, CibSevenWorkflowViewer.elementTypeOf("startEvent"));
    assertEquals(WorkflowElementType.START_EVENT, CibSevenWorkflowViewer.elementTypeOf("messageStartEvent"));
    assertEquals(WorkflowElementType.END_EVENT, CibSevenWorkflowViewer.elementTypeOf("noneEndEvent"));
    assertEquals(WorkflowElementType.END_EVENT, CibSevenWorkflowViewer.elementTypeOf("endEvent"));
    assertEquals(WorkflowElementType.BOUNDARY_EVENT, CibSevenWorkflowViewer.elementTypeOf("boundaryTimer"));
    assertEquals(
        WorkflowElementType.INTERMEDIATE_CATCH_EVENT,
        CibSevenWorkflowViewer.elementTypeOf("intermediateMessageCatch"));
    assertEquals(
        WorkflowElementType.INTERMEDIATE_THROW_EVENT,
        CibSevenWorkflowViewer.elementTypeOf("intermediateSignalThrow"));
    // activities and gateways
    assertEquals(WorkflowElementType.SERVICE_TASK, CibSevenWorkflowViewer.elementTypeOf("serviceTask"));
    assertEquals(WorkflowElementType.USER_TASK, CibSevenWorkflowViewer.elementTypeOf("userTask"));
    assertEquals(WorkflowElementType.SEND_TASK, CibSevenWorkflowViewer.elementTypeOf("sendTask"));
    assertEquals(WorkflowElementType.RECEIVE_TASK, CibSevenWorkflowViewer.elementTypeOf("receiveTask"));
    assertEquals(
        WorkflowElementType.BUSINESS_RULE_TASK,
        CibSevenWorkflowViewer.elementTypeOf("businessRuleTask"));
    assertEquals(WorkflowElementType.SCRIPT_TASK, CibSevenWorkflowViewer.elementTypeOf("scriptTask"));
    assertEquals(WorkflowElementType.MANUAL_TASK, CibSevenWorkflowViewer.elementTypeOf("manualTask"));
    assertEquals(WorkflowElementType.TASK, CibSevenWorkflowViewer.elementTypeOf("task"));
    assertEquals(WorkflowElementType.CALL_ACTIVITY, CibSevenWorkflowViewer.elementTypeOf("callActivity"));
    assertEquals(WorkflowElementType.MULTI_INSTANCE, CibSevenWorkflowViewer.elementTypeOf("multiInstanceBody"));
    assertEquals(WorkflowElementType.EXCLUSIVE_GATEWAY, CibSevenWorkflowViewer.elementTypeOf("exclusiveGateway"));
    assertEquals(WorkflowElementType.INCLUSIVE_GATEWAY, CibSevenWorkflowViewer.elementTypeOf("inclusiveGateway"));
    assertEquals(WorkflowElementType.PARALLEL_GATEWAY, CibSevenWorkflowViewer.elementTypeOf("parallelGateway"));
    assertEquals(
        WorkflowElementType.EVENT_BASED_GATEWAY,
        CibSevenWorkflowViewer.elementTypeOf("eventBasedGateway"));
    assertEquals(WorkflowElementType.SUB_PROCESS, CibSevenWorkflowViewer.elementTypeOf("subProcess"));
    assertEquals(WorkflowElementType.EVENT_SUB_PROCESS, CibSevenWorkflowViewer.elementTypeOf("eventSubProcess"));
    assertEquals(WorkflowElementType.AD_HOC_SUB_PROCESS, CibSevenWorkflowViewer.elementTypeOf("adHocSubProcess"));
    assertEquals(WorkflowElementType.TRANSACTION, CibSevenWorkflowViewer.elementTypeOf("transaction"));
    assertEquals(WorkflowElementType.PROCESS, CibSevenWorkflowViewer.elementTypeOf("processDefinition"));
    assertEquals(WorkflowElementType.SEQUENCE_FLOW, CibSevenWorkflowViewer.elementTypeOf("sequenceFlow"));

  }

  @Test
  @DisplayName("Call activities without a resolvable called process are skipped")
  public void unresolvableCallActivitiesAreSkipped() {

    final var historicInstance = mock(org.cibseven.bpm.engine.history.HistoricProcessInstance.class);
    when(historicInstance.getProcessDefinitionId()).thenReturn("Process:1:aaa");
    when(historicInstanceQuery.list()).thenReturn(List.of(historicInstance));
    when(repositoryService
        .getProcessDefinition("Process:1:aaa")
        .getId())
        .thenReturn("Process:1:aaa");
    when(repositoryService
        .getProcessDefinition("Process:1:aaa")
        .getKey())
        .thenReturn("Process");
    when(repositoryService
        .getProcessDefinition("Process:1:aaa")
        .getVersion())
        .thenReturn(1);

    final var byExpression = mock(org.cibseven.bpm.model.bpmn.instance.CallActivity.class);
    when(byExpression.getCalledElement()).thenReturn("${dynamicProcess}");
    final var blank = mock(org.cibseven.bpm.model.bpmn.instance.CallActivity.class);
    when(blank.getCalledElement()).thenReturn(" ");
    final var notDeployed = mock(org.cibseven.bpm.model.bpmn.instance.CallActivity.class);
    when(notDeployed.getCalledElement()).thenReturn("NotDeployedProcess");
    when(notDeployed.getId()).thenReturn("TheCallActivity");
    when(repositoryService
        .getBpmnModelInstance("Process:1:aaa")
        .getModelElementsByType(org.cibseven.bpm.model.bpmn.instance.CallActivity.class))
        .thenReturn(List.of(byExpression, blank, notDeployed));
    when(definitionQuery.singleResult()).thenReturn(null);

    final var definitions = viewer.getProcessDefinitions("module", "Process", "module", "42", null);

    assertEquals(1, definitions.size(), () -> "only the workflow's own definition is resolvable but got: "
        + definitions);
    assertTrue(definitions
        .getFirst()
        .id()
        .contains("Process:1:aaa"));

  }

}
