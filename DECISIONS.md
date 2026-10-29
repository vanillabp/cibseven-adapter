# Decision log

The code of this repository points at the decisions below. A number is handed out once. It is never
reused or renumbered, so a citation always finds its entry. A decision which gets overturned keeps
its entry. The entry is marked as superseded and names the entry which replaced it.

Most of the code here is made from the Camunda 7 adapter (decision 2). That code cites the decisions
of the Camunda 7 adapter, and it says so: `see decision 7 in the Camunda 7 adapter's DECISIONS.md`.
Code written for this repository cites the entries below, in the usual form
`see decision 2 in the repository's DECISIONS.md`. `bin/check-decision-citations.sh` reads only the
second form, so it checks the entries of this file and nothing else.

The links below point into this repository's [`README.md`](./README.md). The README has the details
an entry leaves out on purpose.

### 1. The decisions of the Camunda 7 adapter apply here, unless an entry below says otherwise

This adapter does what the Camunda 7 adapter does, with CIB seven as the engine. Its code is made
from the Camunda 7 code, and the reasons for that code are written down in the
[decision log of the Camunda 7 adapter](https://github.com/camunda-community-hub/vanillabp-camunda7-adapter/blob/main/DECISIONS.md).
They hold here as well. Copying them would give two texts which drift apart, so they stay there.

An entry below is about one of two things. Either CIB seven differs from Camunda 7, or this
repository differs from the Camunda 7 repository. Where an entry below contradicts a decision of the
Camunda 7 adapter, the entry below wins in this repository.

A change to a decision of the Camunda 7 adapter reaches this repository with the next sync
(decision 2). Whoever runs the sync reads the new entries of that log and asks whether one of them
needs an entry here.

### 2. The code is made from the Camunda 7 adapter, and a merge brings each Camunda 7 change over

CIB seven is a fork of Camunda 7. It renamed the packages and the jars, and it kept the API. So
this adapter is the Camunda 7 adapter with other names, plus a few changes where CIB seven behaves
differently. The Camunda 7 adapter keeps changing, by about fifty pull requests a month, and nearly
all of its changes are wanted here too. A copy made once would be out of date within a week, and
porting each change by hand would cost the most and miss some. So the code is made again from a
commit of the Camunda 7 adapter whenever it is to follow:

- `bin/sync-from-camunda7.sh` copies the Camunda 7 tree and runs the OpenRewrite recipe
  `rewrite/vanillabp-cibseven.yml` over it. The recipe renames the engine's packages and jars,
  the package of the model API's extension elements, which CIB seven renamed on its own, and
  VanillaBP's own names (decision 4). The script commits the result to the branch `generated` as
  `generated from C7 <sha>`.
- `generated` holds nothing else. Nobody edits it by hand. `main` merges it. Everything this
  repository does differently sits on `main` as ordinary commits on top, so a conflict appears
  only where a Camunda 7 change touches one of those places.
- Files which `main` writes itself are never copied: the workflows, `README.md`, `DECISIONS.md`,
  `AGENTS.md`, `CONTRIBUTING.md`, `CLAUDE.md`, `UPGRADE.md` and `renovate.json`. The script lists
  them.

CIB seven publishes a recipe of its own, `org.cibseven.ReplaceCamundaWithCIBSeven`. It is not used,
because it renames every `org.camunda`: the groupId `org.camunda.community.vanillabp` and the FEEL
engine `org.camunda.feel`, which CIB seven shares with Camunda, would change too. And it does not
know the package of the extension elements.

The same commit of the Camunda 7 adapter with the same recipe always gives the same tree. A second
run of the script commits nothing. The workflow `sync-from-camunda7.yaml` runs the script every
night against `main` of the Camunda 7 adapter, and opens a pull request from `generated` when the
result changed.

The recipe renames symbols and names in text. What it cannot see is behaviour: a new Camunda name
in a string which CIB seven changed, a new use of the deployment source, a new engine setting whose
default CIB seven changed. So each sync ends with a search of the Camunda 7 diff for those places.
`CONTRIBUTING.md` holds the procedure and the search.

A test class of the Camunda 7 adapter whose name starts with `C7`, such as `C7E2eAggregate`, keeps
its name. Only classes named `Camunda7...` are renamed.

### 3. One CIB seven minor is pinned, and the adapter version names it

The root POM pins CIB seven in the property `cibseven.version`, at the public release of a minor,
`x.y.0`. The adapter's own version carries the minor: `2.0.0-2.2` is VanillaBP 2.0.0 for CIB seven
2.2. This is the form of the Camunda 8 adapter, which carries the cluster minor the same way.

CIB seven publishes only its minors on Maven Central, two a year. Its patch releases are monthly,
but only for customers of its paid editions, and the previous minor gets them for about five months
after the next minor is out. A patch keeps the API and the database schema. A minor needs a
database upgrade and may break: 2.2 dropped Java 11, 2.3 stops shading the expression language.
The engine runs inside the application, so there is no cluster to match as with Camunda 8. But a
company with a support contract may stay on an older minor for a long time. So:

- There is one line of this adapter, and it follows the newest CIB seven minor. Since the minor is
  part of the version from the start, a second line for an older minor can come later without a
  rename. It comes when a user asks for it.
- An application with a patch release of CIB seven sets that version in its own dependency
  management. That is safe, because a patch keeps the API and the schema.
- An application must not set another minor. The adapter uses engine classes under `.impl.`,
  which CIB seven may change in any minor.
- Renovate keeps the pin inside the minor. A new minor is a story of its own, and the minor in the
  recipe, in the root POM and in the adapter version change together.
- The nightly workflow `nightly-next-cibseven.yaml` builds against the newest CIB seven version on
  Maven Central. A red night opens an issue.

See [Supported CIB seven version](./README.md#supported-cib-seven-version).

### 4. The adapter has a name of its own everywhere: type, package, artifacts and capability

The adapter must be able to run next to the Camunda 7 adapter in one application. That is how an
application moves from Camunda 7 to CIB seven side by side. Two classes with the same name on one
classpath, or two Quarkus extensions with the same capability, break that. So nothing of this
adapter carries the name of the Camunda 7 adapter:

|         What          |           Camunda 7 adapter            |              This adapter              |
|-----------------------|----------------------------------------|----------------------------------------|
| adapter type          | `camunda7`                             | `cibseven`                             |
| Maven groupId         | `org.camunda.community.vanillabp`      | `io.vanillabp`                         |
| artifactIds           | `camunda7-adapter...`                  | `cibseven-adapter...`                  |
| Java package          | `io.vanillabp.camunda7`                | `io.vanillabp.cibseven`                |
| class names           | `Camunda7...`                          | `CibSeven...`                          |
| Quarkus extension     | `vanillabp-camunda7`                   | `vanillabp-cibseven`                   |
| Quarkus capability    | `io.vanillabp.adapter.camunda7`        | `io.vanillabp.adapter.cibseven`        |
| engine and bean names | `vanillabp-camunda7-<id>`, `Camunda7_` | `vanillabp-cibseven-<id>`, `CibSeven_` |

The groupId is not the one of the Camunda Community Hub, because CIB seven is not a Camunda
project. The recipe `rewrite/vanillabp-cibseven.yml` writes all of these names, so a sync keeps
them. `CibSevenSideBySideWithCamunda7IT` runs both adapters in one application.

Some tests of the Camunda 7 adapter put a stand-in for a CIB seven adapter next to it, to see that
two engines on the same tables are refused across adapter types. Here the stand-in would have the
type of the adapter itself, so the recipe turns it into a stand-in for Operaton, the other fork.

### 5. CIB seven's modeler tables are off unless the application switches them on

CIB seven 2.2 added a modeler and an AI chat to the engine, with nine tables of their own: eight
`MOD_*` tables and `CHAT_MESSAGES`. The engine setting `modelerEnabled` is `true` by default. Then
the engine creates the tables, and with `database-schema-update: false` it checks that they are
there. It looks for `MOD_ELEMENT_TEMPLATES` under the table prefix. So an adapter id with a
`table-prefix`, the setup for two engines on one database, does not start on a schema which holds
only the tables of Camunda 7: `ENGINE-03056 Tables are missing for the following components:
[modeler]`.

VanillaBP does not use the modeler. So the key `vanillabp.adapters.<id>.modeler-enabled` is `false`
unless the application sets it, on both platforms, and `CibSevenModeler` writes it into every engine
the adapter builds. An application which wants the modeler sets it to `true`. With a table prefix
it then needs the prefixed modeler tables as well, and the boot check of
`CibSevenTablePrefixSchema` asks for them. Only the adapter level exists, because the setting
belongs to the engine.

The other way would have been to keep the engine's default and teach the boot check the modeler's
table. Then every prefixed setup would need nine tables nobody uses.

### 6. The web application module asks for the secret of CIB seven's webclient, and does not make one up

CIB seven's web application starter for Spring Boot 4 brings two things: the classic web
applications at `/camunda` and the new CIB seven webclient at `/webapp`. The webclient signs its
login tokens with `cibseven.webclient.authentication.jwtSecret`, a base64 text of at least 155
characters. Without a valid one the application does not start, and the failure names neither
this module nor the key.

A secret is not something the adapter may make up. Every instance of an application has to sign
with the same one, and a secret in the code of a library is no secret. So `CibSevenWebclientSecret`
checks the key while the application boots, by the same two rules the webclient applies, and names
the key, the length and a way to get a secret when one of them is broken.

CIB seven answers `GET /camunda` with a page of its own, where Camunda 7 redirects into
`/camunda/app/`. The two tests which asked for the redirect accept the page as well. The classic web
applications themselves are where they were. Whether the new webclient shows the engines of
VanillaBP has not been tested.

### 7. A deployment of the Camunda 7 adapter or of the Operaton adapter counts as one of this adapter's own

CIB seven runs on the database of Camunda 7. CIB seven documents the way: update to Camunda 7.24,
then swap the jars. So an application can stop on the Camunda 7 adapter and start again on this
one, on the same database. The Camunda 7 adapter marks its deployments with the source
`camunda7:<adapter id>`, and this adapter marks them `cibseven:<adapter id>`. A check tells this
adapter's own deployments from those of somebody else by that source. After the switch, every
deployment in the database carries the old mark.

So this adapter reads `cibseven:`, `camunda7:` and `operaton:` as "deployed by VanillaBP". The
Operaton adapter is the other fork of Camunda 7, on the same tables. A check which reports who else
holds a process then does not blame the application's own earlier deployments.

The first start after the switch deploys a new version of every process, because the source
differs and the engine's duplicate filter compares it. Running workflows stay on the version they
started on and finish there. `CibSevenInPlaceSwitchFromCamunda7IT` starts a workflow on the Camunda
7 adapter and finishes it on this one.

### 8. The tests with the Camunda 7 adapter are modules of their own

Some tests run the Camunda 7 adapter in the same application as this adapter: the side-by-side
migration, the refusal of both adapters on the same tables, and the switch of a database. They need
the Camunda 7 adapter on the classpath. They live in two modules of their own, `migration-tests`
for Spring Boot and `quarkus/migration-tests` for Quarkus, and not in the integration test modules,
for two reasons:

- The integration test modules come from the Camunda 7 adapter. Their POMs and configurations
  change with every sync, and a module of this repository's own never meets a conflict there.
- The snapshots of the Camunda 7 adapter are only in the GitHub Packages registry of the Camunda
  Community Hub, which wants a login even for reading. Modules of their own keep that need to two
  places. Every other module builds without the login.

Both modules name that registry under the id `camunda-community-hub-vanillabp-camunda7-adapter`. A
build needs a server entry with that id and a token with `read:packages`. Once the Camunda 7 adapter
has a release on Maven Central, the modules use it and the registry goes away. The coverage report
of each platform reads the execution data of its module.

The modules publish nothing, but each builds its empty jar and leaves itself out of the bundle for
Maven Central with `excludeArtifacts`. The publishing plugin stops the whole publish when a module
of the reactor assigns no file.
