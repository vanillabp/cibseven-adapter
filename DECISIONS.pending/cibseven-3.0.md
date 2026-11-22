### The webclient is the default, and the classic web applications are a module of their own

Decided by the maintainer on 2026-11-22, with the move to CIB seven 3.0.

This replaces decision 6 in part. The secret stays as decision 6 says. Two things change:

- Decision 6 says "The classic web applications themselves are where they were. Whether the new
  webclient shows the engines of VanillaBP has not been tested." Replace these two sentences by:
  "With CIB seven 3.0 the webclient is the default, and the classic web applications need a module
  of their own (decision NN)."
- The check of the secret runs only where the webclient is served. With only the classic web
  applications there is no webclient, and nothing signs a token.

#### What changed in CIB seven 3.0

CIB seven 3.0 deprecated the classic Cockpit, Tasklist and Admin. Its web application starter now
brings only the new webclient at `/webapp`. The classic web applications are a starter of their own,
`cibseven-bpm-spring-boot-starter-webapp-legacy`. That starter brings their classes, but not their
UI files. The UI files are only in `cibseven-webapp-webjar` 2.2.0. Version 3.0.0 of the same
artifact holds the UI of the webclient, and Maven puts only one version of an artifact on the class
path.

#### The decision

1. The key `vanillabp.webapps.ui` chooses the web applications, for the whole application:
   `webclient`, `legacy` or `both`. It is one key for all adapter ids, because both kinds of web
   applications find their engines in the same runtime container and show all of them.
   `vanillabp.adapters.<id>.webapps.enabled` still switches the web applications off for one
   adapter id.
2. The default is `webclient`. An application which moves to CIB seven 3.0 wants the new UI.
3. The classic web applications are in the optional module
   `cibseven-adapter-spring-boot-webapps-legacy`, which the application adds itself. It holds CIB
   seven's legacy starter and a copy of the UI files of `cibseven-webapp-webjar` 2.2.0. The module
   `cibseven-adapter-spring-boot-webapps` does not depend on it. So an application which does not
   want a deprecated UI does not get its jars.
4. `legacy` or `both` without the legacy module ends the start. The message names the coordinates
   of the module and the page of the wiki. With the module and `webclient`, the classic web
   applications stay off, and the log says how to switch them on.

#### The trap for an application of Camunda 7

`camunda.bpm.webapp.application-path` moved Cockpit, Tasklist and Admin with Camunda 7. With CIB
seven it moves the webclient. The classic web applications move with
`camunda.bpm.webapp.legacy-application-path`. An application which set the first key for Camunda 7
finds its webclient at that path, and the classic web applications at `/camunda`, if it added the
legacy module. The wiki says so on the page about the web applications.

#### Rejected

- **The classic web applications in the main module, switched by the key.** Every application would
  carry the jars of a deprecated UI, and every CVE report about them.
- **`both` as the default.** It would keep the old paths working for an application from Camunda 7.
  But the classic web applications are deprecated, and a default would keep them alive longer than
  CIB seven does.
- **A key per adapter id.** Both kinds of web applications show every engine of the runtime
  container, so a choice per adapter id cannot be kept.

#### Where it is cited

- javadoc of `CibSevenWebappsUi` (`DECISIONS.pending/cibseven-3.0.md`, to be replaced by the number)
- the root `pom.xml`, comment on `cibseven-legacy-webjar.version`
- `README.md`, section "CIB seven's web applications"
- the wiki page "Web applications" describes the rule without citing it

