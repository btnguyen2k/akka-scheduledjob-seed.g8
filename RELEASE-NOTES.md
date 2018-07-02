# Release Notes

## 2018-07-02: template-v0.2.0

- Upgrade `ddth-akka` to `v0.1.3`
- `RegisterGlobal`:
  - Support new configuration: cluster mode
  - `application.conf` changes to support cluster mode


## 2018-06-22: template-v0.1.2.2

- Upgrade `ddth-commons` to `v0.9.1.6`.
- `DataSourcesBootstrapper`: accept new optional config `jdbc-driver`


## 2018-06-16: template-v0.1.2.1

- Migrate to `ddth-akka:0.1.2`.
- Config `ddth-akka-scheduling.workers` has simpler format: `<fully-qualified-class-name>[;actor-name;dlock-name]`
- `RegistryGlobal.initWorkers()` updated to work with `ddth-akka-scheduling.workers`' new format.
- Clean-up, bug fixes & enhancements.


## 2018-06-04: template-v0.1.2

- Migrate to `ddth-akka:0.1.1.2`.
- Add more sample workers: `TakeAllTasksWorker`, `LocalSingletonWorker`, `GlobalSingletonWorker`
- Bug fixes.

## 2018-05-12: template-v0.1.1

- Add `RegistryGlobal`
- Add bootstrapper
- Built-in bootstrapper to bootstrap datasource (`DataSourcesBootstrapper`)
- Bug fixes and enhancements


## 2018-05-11: template-v0.1.0

First release:

- Build and package project with `sbt`:
  - Support build standalone package
  - Support build Docker image
  - Support generate Eclipse project
- Support multi-node mode (require Redis server)
- Samples worker implementations in package `com.github.btnguyen2k.akkascheduledjob.samples`
