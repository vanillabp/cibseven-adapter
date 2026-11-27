# Upgrading

VanillaBP 1 had no adapter for CIB seven, so there is nothing to upgrade from in this repository.

- An application which comes from the VanillaBP 1 adapter for Camunda 7 reads the
  [`UPGRADE.md` of the Camunda 7 adapter](https://github.com/vanillabp/camunda7-adapter/blob/main/UPGRADE.md)
  first. Everything it says holds here, with `cibseven` in place of `camunda7`, except for the
  web applications. With VanillaBP 1, Cockpit, Tasklist and Admin were at `/camunda`. Here the
  module `cibseven-adapter-spring-boot-webapps` serves CIB seven's new webclient at `/webapp`.
  To keep the classic web applications, add the module
  `cibseven-adapter-spring-boot-webapps-legacy` and set `vanillabp.webapps.ui` to `legacy` or
  `both`. `camunda.bpm.webapp.application-path` now moves the webclient. The classic web
  applications move with `camunda.bpm.webapp.legacy-application-path`. The
  [wiki page Web applications](https://github.com/vanillabp/cibseven-adapter/wiki/Web-applications)
  has the details.
- An application which moves from Camunda 7 to CIB seven reads
  [Migrating from Camunda 7](./README.md#migrating-from-camunda-7) in the README.

