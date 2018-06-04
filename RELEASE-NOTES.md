# Release Notes

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
