![Header](./readme/vanillabp-headline.png)

# VanillaBP adapter for CIB seven

![Compatible with: CIB seven](https://img.shields.io/badge/Compatible%20with-CIB%20seven-26d07c)
[![Apache License V.2](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

This is the [VanillaBP](https://www.vanillabp.io) Version 2 adapter for
[CIB seven](https://cibseven.org). CIB seven is a fork of Camunda 7, made by CIB software GmbH and
published under the Apache License 2.0. It kept the database schema and the API of Camunda 7. The
packages moved from `org.camunda.bpm` to `org.cibseven.bpm`, and the jars are called `cibseven-*`.
The engine runs embedded in the application, like Camunda 7. Camunda 7 Community Edition ended with
7.24, so an application which wants to stay on an embedded engine of this kind moves to CIB seven
or to Operaton.

If you want to **use** this adapter, read the [Wiki](https://github.com/vanillabp/cibseven-adapter/wiki).
The VanillaBP concepts it builds on are explained in the
[VanillaBP Wiki](https://github.com/vanillabp/adapter-platform-integration/wiki). This `README.md` is
for contributors.

1. [How this adapter relates to the Camunda 7 adapter](#how-this-adapter-relates-to-the-camunda-7-adapter)
2. [Documentation and supported platforms](#documentation-and-supported-platforms)
3. [Coordinates](#coordinates)
4. [Configuration](#configuration)
5. [CIB seven's web applications](#cib-sevens-web-applications)
6. [Supported CIB seven version](#supported-cib-seven-version)
7. [Migrating from Camunda 7](#migrating-from-camunda-7)
8. [Known issues](#known-issues)
9. [Building](#building)
10. [Making the code again from Camunda 7](#making-the-code-again-from-camunda-7)
11. [Test coverage](#test-coverage)

## How this adapter relates to the Camunda 7 adapter

The code of this adapter is made from the
[Camunda 7 adapter](https://github.com/camunda-community-hub/vanillabp-camunda7-adapter) with an
OpenRewrite recipe, `rewrite/vanillabp-cibseven.yml`. The recipe renames what belongs to the engine
and gives the adapter its own names (decision 4 in the repository's DECISIONS.md). A few commits on
top handle what CIB seven does differently. The branch `generated` holds what the recipe made, and
`main` merges it (decision 2 in the repository's DECISIONS.md). The last commit of `generated` says
which commit of the Camunda 7 adapter the code is made from: `generated from C7 <sha>`.

So this adapter behaves like the Camunda 7 adapter, with CIB seven as the engine. The
[README of the Camunda 7 adapter](https://github.com/camunda-community-hub/vanillabp-camunda7-adapter#readme)
describes that behaviour section by section, and every section of it holds here: the task processing,
the outbound operations, the sharing of the workflow aggregate, the versions of a process, Quarkus in
JVM mode only, and the rest. Read "Camunda 7" there as "CIB seven", `camunda7` as `cibseven` and
`Camunda7...` as `CibSeven...`. This README describes only what differs.

The decisions the generated code cites are those of the Camunda 7 adapter. The code says so:
`see decision 32 in the Camunda 7 adapter's DECISIONS.md`. This repository's own decisions are in
[`DECISIONS.md`](./DECISIONS.md), and its first entry says that the others hold here, too.

What differs from the Camunda 7 adapter:

|       Where       |                   Camunda 7 adapter                   |                           This adapter                            |
|-------------------|-------------------------------------------------------|-------------------------------------------------------------------|
| engine            | Camunda 7.24, `org.camunda.bpm`                       | CIB seven 2.2, `org.cibseven.bpm`                                 |
| adapter type      | `camunda7`                                            | `cibseven`                                                        |
| artifacts         | `org.camunda.community.vanillabp:camunda7-adapter...` | `io.vanillabp:cibseven-adapter...`                                |
| version           | `2.0.0`                                               | `2.0.0-2.2`, with the CIB seven minor                             |
| modeler tables    | none                                                  | off by default, key `modeler-enabled`                             |
| web applications  | `/camunda`                                            | `/camunda`, and the webclient needs a secret                      |
| deployment source | `camunda7:<id>`                                       | `cibseven:<id>`, and `camunda7:` and `operaton:` count as its own |

The BPMN namespace does not change. CIB seven reads only `http://camunda.org/schema/1.0/bpmn`, the
`camunda:` attributes of a model written for Camunda 7, and has no namespace of its own.

## Documentation and supported platforms

This adapter runs on both platforms VanillaBP supports:

1. **Spring Boot**<br>[![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fcibseven-adapter%2Fspring-boot-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/cibseven-adapter/spring-boot-report)
2. **Quarkus**, in JVM mode only<br>[![Coverage](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fvanillabp.github.io%2Fcibseven-adapter%2Fquarkus-report%2Findex.html&search=Total.*%3F.([0-9]%2B)[^0-9]*%3F%25&replace=%241%25&flags=m&label=Coverage&color=green&cacheSeconds=60)](https://vanillabp.github.io/cibseven-adapter/quarkus-report)

Coverage is measured separately per platform. The tests of one platform never cover the code of the
other. Click a badge to open its report.

The adapter builds the plain engine itself, on both platforms. It uses neither CIB seven's Spring
Boot starter nor CIB seven's Quarkus extension, so their versions of Spring Boot and Quarkus do not
bind the application. On Spring Boot it uses `cibseven-engine-spring-7`, which is built for Spring
7 and so for Spring Boot 4. On Quarkus it builds the engine like the Camunda 7 adapter does, on
Agroal and Narayana. Only JVM mode works, as with Camunda 7.

## Coordinates

Spring Boot:

```xml
<dependency>
  <groupId>io.vanillabp</groupId>
  <artifactId>cibseven-adapter-spring-boot</artifactId>
  <version>2.0.0-2.2-SNAPSHOT</version>
</dependency>
```

Quarkus, where VanillaBP and the adapter are both extensions:

```xml
<dependency>
  <groupId>io.vanillabp</groupId>
  <artifactId>vanillabp-quarkus-integration</artifactId>
  <version>2.0.0-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>io.vanillabp</groupId>
  <artifactId>cibseven-adapter-quarkus</artifactId>
  <version>2.0.0-2.2-SNAPSHOT</version>
</dependency>
```

|           Module            |                Artifact                |                        Contents                         |
|-----------------------------|----------------------------------------|---------------------------------------------------------|
| `core`                      | `cibseven-adapter`                     | Platform-neutral SPI implementations and engine wiring. |
| `spring-boot`               | `cibseven-adapter-spring-boot`         | Spring Boot auto-configuration registering the adapter. |
| `spring-boot-webapps`       | `cibseven-adapter-spring-boot-webapps` | Optional: CIB seven's Cockpit, Tasklist and Admin.      |
| `quarkus/runtime`           | `cibseven-adapter-quarkus`             | The Quarkus extension.                                  |
| `quarkus/deployment`        | `cibseven-adapter-quarkus-deployment`  | The build steps of the Quarkus extension.               |
| `integration-tests`         | (not published)                        | Spring Boot tests against the real engine.              |
| `quarkus/integration-tests` | (not published)                        | Quarkus tests against the real engine.                  |
| `migration-tests`           | (not published)                        | This adapter next to the Camunda 7 adapter, decision 8. |
| `quarkus/migration-tests`   | (not published)                        | The same on Quarkus.                                    |

The class names follow the Camunda 7 adapter with `CibSeven` in place of `Camunda7`. An extension
which works with this adapter, such as the Business Cockpit, uses for example `CibSevenAdapter`,
`CibSevenProcessingContext`, `CibSevenEngineFacts`, `CibSevenMultiInstances`,
`CibSevenTaskDefinitions`, `CibSevenEngineCustomizer`, `CibSevenEngineHolder`,
`CibSevenQuarkusEngineRegistry`, `CibSevenProcessVersions`, `CibSevenTaskRegistry` and
`VanillaBpCibSevenProperties`. They are in the package `io.vanillabp.cibseven` and below.

## Configuration

The adapter is a VanillaBP adapter of type `cibseven`. If CIB seven is the only BPMS of the
application, adding the dependency is all it takes: the adapter id is then the type, `cibseven`, and
the BPMN files are read from `<workflow-module-id>/processes/cibseven`. To configure an instance,
give it an id and set its `type`:

```yaml
vanillabp:
  prioritized-adapters:
    - cs
  adapters:
    cs:
      type: cibseven
      history-time-to-live: P30D
```

The keys are the ones of the Camunda 7 adapter, at `vanillabp.adapters.<id>.*`, and they are named
after the setters of the engine configuration, which CIB seven kept:
`database-schema-update`, `history-time-to-live`, `data-source-name`, `table-prefix`,
`serialization-format`, `engine-plugins`, `tenant-id`, `accept-unscoped-identifiers`,
`allow-listeners`, `sleep-until-something-is-due`, `db-metrics-reporting` and
`dmn-feel-legacy-behavior`. The [README of the Camunda 7 adapter](https://github.com/camunda-community-hub/vanillabp-camunda7-adapter#configuration)
describes each of them. The adapter reads none of CIB seven's own keys (`camunda.bpm.*`,
`quarkus.cibseven.*`), because it does not use CIB seven's starter or extension.

One key is new:

|        Key        | Default |                                                                  What it does                                                                  |
|-------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `modeler-enabled` | `false` | Whether the engine creates and checks the tables of CIB seven's modeler and AI chat (`MOD_*`, `CHAT_MESSAGES`). Only the adapter level exists. |

CIB seven 2.2 added a modeler with tables of its own, and the engine switches it on by default. With
`database-schema-update: false` the engine then checks for the modeler's tables under the table
prefix, so a prefixed adapter id would not start on a schema with the tables of Camunda 7 only.
VanillaBP does not use the modeler, so the adapter switches it off (decision 5 in the repository's
DECISIONS.md). An application which wants it sets `modeler-enabled: true`. With a `table-prefix` it
then needs the prefixed modeler tables, too, and the boot check says so when they are missing.

Each adapter id gets its own embedded engine, named `vanillabp-cibseven-<id>`. Two engines must not
share one set of tables. That holds across adapter types, too: a `cibseven` id and a `camunda7` or
`operaton` id on the same datasource without a `table-prefix` stop the start with a message which
names both ids (decision 47 in the Camunda 7 adapter's DECISIONS.md). `CibSevenSideBySideWithCamunda7IT`
tests it with the real Camunda 7 adapter.

An engine plugin is named by its CIB seven class, for example SPIN:

```yaml
vanillabp:
  adapters:
    cs:
      serialization-format: application/json
      engine-plugins:
        spin:
          plugin-class: org.cibseven.spin.plugin.impl.SpinProcessEnginePlugin
```

The dependencies are `org.cibseven.bpm:cibseven-engine-plugin-spin` and
`org.cibseven.spin:cibseven-spin-dataformat-json-jackson`. `camunda-xstream` is built against
Camunda's packages and does not run on CIB seven, so `application/xstream` needs a plugin of your
own.

## CIB seven's web applications

The optional module `cibseven-adapter-spring-boot-webapps` serves Cockpit, Tasklist and Admin at
`/camunda` against the engines this adapter built. It works like the module of the Camunda 7
adapter: CIB seven's starter still reads `camunda.bpm.*`, so the module switches
`camunda.bpm.enabled` off, and it registers VanillaBP's engines with the runtime container.

Two things differ:

1. **The webclient needs a secret.** CIB seven's starter for Spring Boot 4,
   `cibseven-bpm-spring-boot-starter-webapp-4`, also brings CIB seven's new webclient, at `/webapp`.
   It signs its login tokens with `cibseven.webclient.authentication.jwtSecret` and does not start
   without it. The module does not make up a secret, because every instance of the application has
   to use the same one (decision 6 in the repository's DECISIONS.md). It checks the key while the
   application boots. Set it to a base64 text of at least 155 characters, for example:

   ```bash
   openssl rand -base64 120 | tr -d '\n'
   ```

   ```yaml
   cibseven:
     webclient:
       authentication:
         jwtSecret: ${WEBCLIENT_JWT_SECRET}
   ```
2. **`/camunda` does not redirect.** CIB seven answers `GET /camunda` with a page of its own. The
   classic web applications are at `/camunda/app/cockpit/`, `/camunda/app/tasklist/` and
   `/camunda/app/admin/`, as before.

Whether the new webclient at `/webapp` shows the engines of VanillaBP has **not been tested**. The
web applications of the Camunda 7 adapter and of this adapter in one application are not supported:
both starters read `camunda.bpm.*` and both want `/camunda`. On Quarkus there are no web
applications, as with Camunda 7.

`CibSevenWebappsBootTest` and `CibSevenWebclientSecretTest` test this module.

## Supported CIB seven version

|   Adapter version    | CIB seven version it was built and tested with |
|----------------------|------------------------------------------------|
| `2.0.0-2.2-SNAPSHOT` | 2.2.0                                          |

The root POM pins CIB seven in the property `cibseven.version`, and the adapter version carries the
CIB seven minor, like the Camunda 8 adapter carries the cluster minor (decision 3 in the
repository's DECISIONS.md). There is one line of this adapter, and it follows the newest CIB seven
minor:

- CIB seven publishes only its minors on Maven Central, two a year. Its patch releases (`2.2.1`, ...)
  are for the customers of its paid editions. **A patch is safe to use**: it keeps the API and the
  database schema. An application with a patch sets the version in its own dependency management,
  for instance with the `org.cibseven.bpm:cibseven-bom` of that patch.
- **Do not set another minor.** The adapter uses engine classes under `.impl.`, and CIB seven may
  change them in any minor. Each minor also needs a database upgrade.
- Because the minor is part of the version, a second line for an older minor can come later
  without a rename. It comes when a user asks for it.
- Renovate keeps the pin inside the minor. A nightly build runs the whole suite against the newest
  CIB seven version on Maven Central (`.github/workflows/nightly-next-cibseven.yaml`). A red night
  opens an issue labelled `nightly-next-cibseven`.

CIB seven 2.2 runs on Java 17 and 21. The adapter is built for Java 21, like VanillaBP.

## Migrating from Camunda 7

There are two ways, and both keep the BPMN models as they are.

### In place

This is the way CIB seven documents in "Camunda Migration" of its manual: first update to Camunda
7.24, then swap the jars. The `ACT_*` tables stay. With VanillaBP:

1. Update to the Camunda 7 adapter on Camunda 7.24 first.
2. Replace the dependency of the Camunda 7 adapter by the one of this adapter.
3. Change the `type` of the adapter id from `camunda7` to `cibseven`. Keep the id, and keep
   `data-source-name` and `table-prefix` as they were.
4. Replace class names of Camunda in your configuration, for example the plugin class
   `org.camunda.spin.plugin.impl.SpinProcessEnginePlugin` by
   `org.cibseven.spin.plugin.impl.SpinProcessEnginePlugin`.

CIB seven's upgrade scripts from 7.24 to 2.1 and from 2.1 to 2.2 add a row to the schema log and
the tables of the modeler. They are in `cibseven-engine-2.2.0.jar!/org/cibseven/bpm/engine/db/upgrade/`.
Run them as CIB seven's manual says. The modeler's tables are only used with
`modeler-enabled: true`. `CibSevenInPlaceSwitchFromCamunda7IT` starts this adapter on the tables the
Camunda 7 adapter wrote, with `database-schema-update: true`, and the engine starts without them.

The first start deploys a **new version of every process**. The deployments of the Camunda 7
adapter carry the source `camunda7:<id>`, this adapter's carry `cibseven:<id>`, and the engine's
duplicate filter compares the source. Running workflows stay on their version and finish there. This
adapter reads both sources as its own, so its checks do not report the old deployments as somebody
else's (decision 7 in the repository's DECISIONS.md). `CibSevenInPlaceSwitchFromCamunda7IT` starts a
workflow on the Camunda 7 adapter and finishes it here.

Data which names a Camunda class stays as it is. That is, for example, a Java object which the
engine stored serialized, or a script which names `org.camunda` classes. CIB seven has the engine
settings `useCibSevenNamespaceInReflection` and `useCibSevenNamespaceInScripting` for that. The
adapter itself writes no class names into the database.

### Side by side

VanillaBP's migration feature runs two adapters next to each other: the Camunda 7 adapter keeps
serving the workflows which run on it, and new workflows start on CIB seven. The application lists
both, CIB seven first:

```yaml
vanillabp:
  prioritized-adapters:
    - cs
    - c7
  adapters:
    cs:
      type: cibseven
      data-source-name: cibsevenDataSource
    c7:
      type: camunda7
```

The two engines need tables of their own: a datasource of their own (`data-source-name`, as above)
or a `table-prefix` of their own. On the same tables, the start stops with a message which names
both adapter ids.

`CibSevenSideBySideWithCamunda7IT` runs this on Spring Boot, and `CibSevenSideBySideWithCamunda7Test`
on Quarkus. On Spring Boot the application runs twice on the same
databases, first with Camunda 7 in front, then with CIB seven in front. The workflow started before
the switch finishes on Camunda 7, the new one runs on CIB seven, and each engine runs a FEEL
decision. Both engines use the same FEEL engine library, `org.camunda.feel:feel-engine` 1.19.3 with
the classifier `scala-shaded`, so one copy on the classpath serves both. Check this again with each
new CIB seven minor.

**On Quarkus, both datasources have to be XA datasources** (`quarkus.datasource.<name>.jdbc.transactions: xa`).
A task of the engine on the second datasource loads the workflow aggregate from the application's
datasource in the same transaction. Without XA, Narayana refuses the second datasource: the task
handler runs, but the job of the engine rolls back. A start through the outbox fails the same way.
This comes from the Camunda 7 adapter and holds for it as well.

## Known issues

- **The new webclient is untested**, see [CIB seven's web applications](#cib-sevens-web-applications).
- **Web applications of both engines in one application** are not supported.
- **CIB seven 2.3 stops shading the expression language.** Its engine then uses `jakarta.el`
  directly. The recipe needs one more rule for it, the adapter needs `jakarta.el-api`, and a test
  with Spring Boot's own expression language next to it. That is a story of its own, with the move
  to 2.3.

## Building

You need a JDK 21 or newer, and Maven. Then:

```bash
mvn spotless:apply
mvn install
```

Use `install`, not `verify`: the Quarkus tests start the extension from the local Maven repository.
`spi-for-java` and the platform come from the snapshot repository of Maven Central. The module
`migration-tests` also needs the Camunda 7 adapter, whose snapshots need a login, see
[`CONTRIBUTING.md`](./CONTRIBUTING.md#snapshots). The engine runs embedded on H2, so the tests need
no Docker.

Every push to `main` publishes the snapshots to the snapshot repository of Maven Central, and the
coverage reports to GitHub Pages (`.github/workflows/publish-snapshots.yaml`). Two builds run every
night: one against the newest platform snapshot (`nightly-platform-snapshot.yaml`) and one against
the newest CIB seven version (`nightly-next-cibseven.yaml`). A red night opens an issue.

## Making the code again from Camunda 7

One command makes the code again from a commit of the Camunda 7 adapter and commits the result to
the branch `generated`:

```bash
bin/sync-from-camunda7.sh <tag or sha of the Camunda 7 adapter>
```

`C7_REPOSITORY` names another clone, a local path works too. The same commit gives the same tree, so
a second run commits nothing. Then `main` merges `generated`. [`CONTRIBUTING.md`](./CONTRIBUTING.md#following-the-camunda-7-adapter)
describes the steps, and what to look for in the Camunda 7 diff. The workflow
`sync-from-camunda7.yaml` does the first part every night against `main` of the Camunda 7 adapter,
and opens a pull request when the result changed.

## Test coverage

`mvn install` builds one aggregated JaCoCo report per platform, into
`test-coverage-report/spring-boot/report` and `test-coverage-report/quarkus/report`. The module
`test-coverage-report/coverage-gate` fails the build below 85 percent of covered instructions per
platform. The rule is 90. Both work as in the Camunda 7 adapter, whose README says more.

## License

VanillaBP is published under the [Apache License, Version 2.0](./LICENSE). CIB seven is published
under the Apache License, Version 2.0, too.
