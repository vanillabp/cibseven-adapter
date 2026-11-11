package io.vanillabp.cibseven.springboot;

import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.cibseven.bpm.engine.ProcessEngine;
import org.cibseven.bpm.engine.RepositoryService;
import org.cibseven.bpm.engine.RuntimeService;
import org.springframework.beans.factory.BeanRegistrar;
import org.springframework.beans.factory.BeanRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

import io.vanillabp.cibseven.deployment.CibSevenDeploymentService;
import io.vanillabp.cibseven.engine.CibSevenEngineProperties;
import io.vanillabp.cibseven.engine.CibSevenInstanceIdentity;
import io.vanillabp.cibseven.processservice.CibSevenProcessService;
import io.vanillabp.cibseven.springboot.engine.CibSevenEngineHolder;
import io.vanillabp.integration.adapter.AdapterBeanRegistrarSupport;
import io.vanillabp.integration.adapter.spi.workflowtask.WorkflowTaskInvoker;
import io.vanillabp.integration.config.EmptySections;

/**
 * Registers the beans of the CIB seven adapter, per adapter id. Several ids of one BPMS
 * type are the migration scenario, e.g. two embedded engines side by side on separate
 * schemas. For EACH configured adapter id of type {@code cibseven} it registers:
 * <ul>
 * <li>one {@link CibSevenEngineHolder} bean. It owns THAT id's embedded engine, named
 * <code>vanillabp-cibseven-&lt;id&gt;</code>. It also runs the lifecycle of the job
 * executor and, if there is one, of the id's own datasource.</li>
 * <li>one {@link CibSevenProcessService} <i>element</i> bean and one
 * {@link CibSevenDeploymentService} <i>element</i> bean.</li>
 * <li>named convenience beans {@link ProcessEngine}, {@link RuntimeService} and
 * {@link RepositoryService}.</li>
 * <li>one {@link io.vanillabp.cibseven.api.CibSevenEngineFacts} bean. An extension of this
 * adapter asks it about the engine it runs in.</li>
 * </ul>
 * It never registers beans of type {@code List<...>}. The platform collects element beans
 * through {@code ObjectProvider.stream()}.
 * <p>
 * The set of ids comes from the runtime configuration. So the beans are registered in
 * code, through {@link BeanRegistrar} and
 * {@link AdapterBeanRegistrarSupport#forEachConfiguredAdapterId}. The adapter id is a
 * CONSTRUCTOR parameter of each instance. Without a configured {@code cibseven} adapter id
 * no bean is registered at all. So an application which only has the adapter jar on the
 * classpath gets no {@code ACT_*} tables and no job executor.
 * <p>
 * <b>Startup validation:</b> two embedded engines on one schema share the same engine
 * state. The adapter SPI hook
 * {@code AdapterDeploymentService#validateDistinctAdapterInstances} checks that. It is
 * implemented ONCE, in {@link CibSevenDeploymentService}, for both platforms. Adapter ids
 * which share one datasource AND one table prefix fail the boot with a guiding message. An
 * application which provides SEVERAL datasources also has to name the one an adapter id
 * runs on (see {@link #applicationBean}).
 */
public class CibSevenAdapterBeanRegistrar implements BeanRegistrar {

  /**
   * Spring Boot builds the class when the auto-configuration imports it. It holds nothing.
   * It reads the adapter ids from the environment while it registers.
   */
  public CibSevenAdapterBeanRegistrar() {

  }

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory
      .getLogger(CibSevenAdapterBeanRegistrar.class);

  @Override
  public void register(
      final BeanRegistry registry,
      final Environment environment) {

    final var cibsevenAdapterIds = new LinkedList<String>();
    AdapterBeanRegistrarSupport.forEachConfiguredAdapterId(
        environment,
        CibSevenAdapterConfiguration.ADAPTER_TYPE,
        cibsevenAdapterIds::add);

    // a key of Camunda's Spring Boot starter which nobody reads any more would change
    // nothing, and nobody would notice. So it ends the start, with what to write instead
    if (!cibsevenAdapterIds.isEmpty()) {
      CibSevenVersionOneKeys.endTheStartOnKeysOfVersionOne(
          cibsevenAdapterIds,
          propertyNames(environment),
          CibSevenVersionOneKeys.webappsModuleIsPresent(CibSevenAdapterBeanRegistrar.class.getClassLoader()),
          CibSevenVersionOneKeys.externalTaskClientIsPresent(CibSevenAdapterBeanRegistrar.class.getClassLoader()));
    }

    cibsevenAdapterIds
        .forEach(adapterId -> {

          registry.registerBean(
              "CibSeven_Engine_%s".formatted(adapterId),
              CibSevenEngineHolder.class,
              spec -> spec.supplier(supplierContext -> {
                final var engineProperties = enginePropertiesFor(environment, adapterId);
                // the application's DEFAULT DataSource and PlatformTransactionManager
                // are only needed, and only have to exist, if the id names no
                // datasource bean (data-source-name)
                final var usesNamedDataSource = engineProperties.usesSeparateDataSource();
                return new CibSevenEngineHolder(
                    adapterId, engineProperties, usesNamedDataSource
                        ? null
                        : () -> applicationBean(supplierContext, DataSource.class, adapterId), usesNamedDataSource
                            ? null
                            : () -> applicationBean(supplierContext, PlatformTransactionManager.class,
                                adapterId), supplierContext
                                    .bean(WorkflowTaskInvoker.class), supplierContext
                                        .beanProvider(
                                            io.vanillabp.integration.adapter.spi.workflowstart.BpmsInitiatedStartInvoker.class)
                                        .getIfAvailable(), supplierContext
                                            .beanProvider(
                                                io.vanillabp.integration.adapter.spi.workflowend.WorkflowEndedInvoker.class)
                                            .getIfAvailable());
              }));

          registry.registerBean(
              "CibSeven_ProcessService_%s".formatted(adapterId),
              CibSevenProcessService.class,
              spec -> spec.supplier(supplierContext -> {
                final var engine = engineHolder(supplierContext, adapterId);
                final var processService = new CibSevenProcessService<>(
                    adapterId, engine.getRuntimeService(), engine.getTaskService(), engine
                        .getRepositoryService(), engine.getHistoryService(), AdapterBeanRegistrarSupport
                            .collaborators(supplierContext, adapterId));
                // an engine on a datasource of its own commits apart from the
                // application. That makes its deliveries repeatable
                processService.setEngineRunsOnItsOwnDataSource(engine.usesSeparateDataSource());
                // a start reports the version of its process definition. It reads it
                // from the same cache the deliveries of this engine use
                processService.setTaskRegistry(engine.getTaskRegistry());
                processService
                    .setConfiguredTenants(
                        configuredTenantsOf(supplierContext.bean(VanillaBpCibSevenProperties.class), adapterId));
                // which serialization format nested shared values are stored in. It is
                // resolved per workflow, with a fallback to the module and the adapter
                final var overlay = supplierContext.bean(VanillaBpCibSevenProperties.class);
                processService.setSerializationFormats(
                    (
                        workflowModuleId,
                        bpmnProcessId) -> overlay
                            .serializationFormatFor(adapterId, workflowModuleId, bpmnProcessId));
                engine
                    .getTaskRegistry()
                    .setSerializationFormats(
                        (
                            workflowModuleId,
                            bpmnProcessId) -> overlay
                                .serializationFormatFor(adapterId, workflowModuleId, bpmnProcessId));
                // how long a correlation waits for a subscription. The platform resolves it
                // per message with a fallback to the workflow and the module, the same for
                // every adapter. A negative value is refused here, at the start, and not when the first
                // message is correlated
                final var coreProperties = supplierContext
                    .bean(io.vanillabp.integration.adapter.migration.config.MigrationAdapterProperties.class);
                CibSevenProcessService
                    .refuseNegativeMessageTimesToLive(adapterId,
                        coreProperties.messageTimesToLiveWrittenFor(adapterId));
                processService.setMessageTimeToLiveResolver(coreProperties.messageTimeToLiveResolver());
                return processService;
              }));

          registry.registerBean(
              "CibSeven_DeploymentService_%s".formatted(adapterId),
              CibSevenDeploymentService.class,
              spec -> spec.supplier(supplierContext -> {
                final var engine = engineHolder(supplierContext, adapterId);
                final var deploymentService = new CibSevenDeploymentService(
                    adapterId, engine.getRepositoryService(), engine, AdapterBeanRegistrarSupport
                        .collaborators(supplierContext,
                            adapterId), engine.getTaskRegistry(), instanceIdentities(supplierContext));
                deploymentService.setEngineDeliversWorkflowEnded(engine.deliversWorkflowEnded());
                deploymentService.setConfiguredTenants(
                    configuredTenantsOf(supplierContext.bean(VanillaBpCibSevenProperties.class), adapterId));
                deploymentService.setIdentityService(
                    engine
                        .getProcessEngine()
                        .getIdentityService());
                // where every version of a workflow module is registered with the job executor
                deploymentService.setManagementService(
                    engine
                        .getProcessEngine()
                        .getManagementService());
                // How many workflows still run on an older version
                deploymentService.setRuntimeService(
                    engine
                        .getProcessEngine()
                        .getRuntimeService());
                deploymentService.setAcceptUnscopedIdentifiers(
                    supplierContext
                        .bean(VanillaBpCibSevenProperties.class)
                        .enginePropertiesFor(adapterId)
                        .isAcceptUnscopedIdentifiers());
                // which format a workflow's values travel in, and what this engine's
                // serializers make of them. The startup check reads both
                final var deploymentOverlay = supplierContext.bean(VanillaBpCibSevenProperties.class);
                deploymentService.setSerializationFormats(
                    (
                        workflowModuleId,
                        bpmnProcessId) -> deploymentOverlay
                            .serializationFormatFor(adapterId, workflowModuleId, bpmnProcessId));
                deploymentService.setSerializationRoundTrip(
                    io.vanillabp.cibseven.sync.CibSevenSerializationRoundTrip.of(engine.getProcessEngine()));
                // whether this application serves the listeners somebody modelled. It
                // can be set down to the workflow
                deploymentService.setAllowListenersResolver(
                    (
                        workflowModuleId,
                        bpmnProcessId) -> deploymentOverlay
                            .allowListenersFor(adapterId, workflowModuleId, bpmnProcessId));
                // a Spring bean named like a task definition takes the expression away
                // from the method. The startup check asks the context the engine asks
                deploymentService.setClassOfTheBeanNamed(engine::classOfTheBeanNamed);
                // a key at a level which does not resolve it changes nothing. Silence
                // would be worse than a line which says where the key is read
                io.vanillabp.cibseven.wiring.CibSevenListeners.reportKeysSetAtTaskLevel(
                    adapterId,
                    deploymentOverlay.allowListenersKeysAtTaskLevel(adapterId),
                    log::warn);
                return deploymentService;
              }));

          // what this adapter knows about THIS engine, for an extension which runs in it.
          // One CibSevenEngineFacts per adapter id. It is built from what the engine
          // wiring computed anyway, not from a second reading of the configuration
          registry.registerBean(
              "CibSeven_EngineFacts_%s".formatted(adapterId),
              io.vanillabp.cibseven.api.CibSevenEngineFacts.class,
              spec -> spec.supplier(supplierContext -> new io.vanillabp.cibseven.api.CibSevenEngineFacts(
                  adapterId, AdapterBeanRegistrarSupport
                      .collaborators(supplierContext, adapterId)
                      .scoping(), configuredTenantsOf(
                          supplierContext.bean(VanillaBpCibSevenProperties.class), adapterId), engineHolder(
                              supplierContext, adapterId)
                              .getTaskRegistry())));

          // named convenience beans, e.g. for tests and applications integrating
          // with the engine directly (unique by type in single-adapter setups)
          registry.registerBean(
              "CibSeven_ProcessEngine_%s".formatted(adapterId),
              ProcessEngine.class,
              spec -> spec.supplier(supplierContext -> engineHolder(supplierContext, adapterId).getProcessEngine()));
          registry.registerBean(
              "CibSeven_RuntimeService_%s".formatted(adapterId),
              RuntimeService.class,
              spec -> spec.supplier(supplierContext -> engineHolder(supplierContext, adapterId).getRuntimeService()));
          registry.registerBean(
              "CibSeven_RepositoryService_%s".formatted(adapterId),
              RepositoryService.class,
              spec -> spec
                  .supplier(supplierContext -> engineHolder(supplierContext, adapterId).getRepositoryService()));

        });

  }

  private static CibSevenEngineHolder engineHolder(
      final BeanRegistry.SupplierContext supplierContext,
      final String adapterId) {

    return supplierContext.bean("CibSeven_Engine_%s".formatted(adapterId), CibSevenEngineHolder.class);

  }

  /**
   * Binds the adapter id's engine settings from the canonical per-adapter location
   * <code>vanillabp.adapters.&lt;id&gt;.*</code> (the overlay view, see
   * {@link VanillaBpCibSevenProperties}). A section written as <code>{}</code> keeps the
   * keys other property sources set below it, because the platform's
   * {@link EmptySections#bindHandler()} takes part in the binding.
   */
  static CibSevenEngineProperties enginePropertiesFor(
      final Environment environment,
      final String adapterId) {

    return Binder
        .get(environment)
        .bind(
            "vanillabp.adapters.%s".formatted(adapterId),
            Bindable.of(CibSevenEngineProperties.class),
            EmptySections.bindHandler())
        .orElseGet(CibSevenEngineProperties::new);

  }

  /**
   * The names of every property a configuration file or a property source with a list of
   * names holds. A source which cannot list its names is skipped. So is a source which a
   * webapps module of VanillaBP adds itself, see
   * {@link CibSevenVersionOneKeys#isAddedByAWebappsModule}.
   */
  private static List<String> propertyNames(
      final Environment environment) {

    if (!(environment instanceof org.springframework.core.env.ConfigurableEnvironment configurable)) {
      return List.of();
    }
    return configurable
        .getPropertySources()
        .stream()
        .filter(org.springframework.core.env.EnumerablePropertySource.class::isInstance)
        .filter(source -> !CibSevenVersionOneKeys.isAddedByAWebappsModule(source.getName()))
        .flatMap(source -> java.util.Arrays
            .stream(((org.springframework.core.env.EnumerablePropertySource<?>) source).getPropertyNames()))
        .toList();

  }

  /**
   * Resolves a bean the application provides. If that fails, it fails with a GUIDING
   * message, not with a bean-wiring error. The embedded engine needs a {@code DataSource}
   * and a {@code PlatformTransactionManager}. A configured adapter without a database is a
   * configuration defect. The developer has to learn about it with the remedy named, not
   * through a {@code NoSuchBeanDefinitionException}. Several datasources are refused before
   * this is asked, see {@link io.vanillabp.cibseven.springboot.engine.CibSevenEngineDatabase#choose}.
   *
   * @param <S> The bean type
   * @param supplierContext The bean supplier context
   * @param beanType The bean type
   * @param adapterId The adapter ID (used in the guiding message)
   * @return The bean
   */
  private static <S> S applicationBean(
      final BeanRegistry.SupplierContext supplierContext,
      final Class<S> beanType,
      final String adapterId) {

    final var bean = supplierContext
        .beanProvider(beanType)
        .getIfAvailable();
    if (bean == null) {
      throw new IllegalStateException(
          """
              CIB seven adapter '%s' is configured ('vanillabp.adapters.%s.type: cibseven') but the \
              embedded engine cannot be created: no DataSource/PlatformTransactionManager is available. \
              CIB seven runs embedded and always needs a database - configure 'spring.datasource.*' \
              (and a transaction manager, e.g. via the JDBC or JPA starters), reference an \
              application-provided DataSource bean via 'vanillabp.adapters.%s.data-source-name' or \
              remove the adapter configuration."""
              .formatted(adapterId, adapterId, adapterId));
    }
    return bean;

  }

  /**
   * What makes an adapter id a distinct engine: the datasource its engine really runs on,
   * and its table prefix (see {@link CibSevenInstanceIdentity}). It is asked of the engine
   * holders, not read from the keys. A bean <code>camundaBpmDataSource</code> takes the
   * place of the default datasource without any key, and a <code>data-source-name</code>
   * may name the default datasource by its bean name. The check of the deployment pipeline
   * would miss both, if it compared keys. See decision 60 in the Camunda 7 adapter's DECISIONS.md.
   */
  private static java.util.function.Function<String, CibSevenInstanceIdentity> instanceIdentities(
      final BeanRegistry.SupplierContext supplierContext) {

    final var engineHolders = supplierContext.beanProvider(CibSevenEngineHolder.class);
    return adapterId -> engineHolders
        .stream()
        .filter(holder -> holder
            .getAdapterId()
            .equals(adapterId))
        .findFirst()
        .map(CibSevenEngineHolder::getInstanceIdentity)
        .orElseThrow(() -> new IllegalStateException(
            "There is no CIB seven engine for the adapter id '%s'".formatted(adapterId)));

  }

  /**
   * The configured tenant of each workflow module. It is read per workflow module
   * (<code>vanillabp.workflow-modules.&lt;module&gt;.adapters.&lt;id&gt;.tenant-id</code>),
   * with a fallback to the adapter (<code>vanillabp.adapters.&lt;id&gt;.tenant-id</code>).
   * It is <code>null</code> for a module which neither names. Then the workflow module id
   * names the tenant.
   */
  private static java.util.function.Function<String, io.vanillabp.cibseven.wiring.CibSevenConfiguredTenant> configuredTenantsOf(
      final VanillaBpCibSevenProperties overlay,
      final String adapterId) {

    return workflowModuleId -> overlay.configuredTenantFor(adapterId, workflowModuleId);

  }

}
