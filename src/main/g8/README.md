# $name$

$desc$ by $organization$, based from [akka-scheduledjob-seed.g8](https://github.com/btnguyen2k/akka-scheduledjob-seed.g8).

Copyright (C) by $organization$.

Latest release version: `0.1.0`. See [RELEASE-NOTES.md](RELEASE-NOTES.md).

## Usage

Generate Eclipse project with: `sbt eclipse`

Build standalone `.zip` distribution package: `sbt universal:packageBin`

Build Docker package: `sbt docker:stage`

Build Docker image and publish to local: `sbt docker:publishLocal`

See more: http://www.scala-sbt.org/sbt-native-packager/formats/universal.html

**Configuration file**

Application's main configuration file `conf/application.conf` in [HOCON format](https://github.com/lightbend/config/blob/master/HOCON.md).

Important configurations:

```
## Application name and version
app {
    version   = "0.1.0"
    name      = "application-name-in-lower-cases-without-special-characterss"
    shortname = "app_short_name"
    fullname  = ${app.name} ${app.version}
    desc      = "Application description, free text"
}
```

```
## ddth-akka-scheduling configurations
ddth-akka-scheduling {
    # set to "true" to enable multi-mode, "false" otherwise
    multi-node-mode     = true

    # multi-node mode: d-lock time in milliseconds
    dlock-time-ms       = 5000

    # multi-node mode: sleep time between queue poll in milliseconds
    queue-poll-sleep-ms = 1000

    # used in multi-mode only
    dlock-backend {
        # either "local" or "redis", "local" type has no more settings
        type                = "redis"
        lock-name           = "akka-scheduled-job"
        # redis settings
        redis-host-and-port = "localhost:6379"
        redis-password      = ""
    }

    # used in multi-mode only
    queue-backend {
        # either "local" or "redis", "local" type has no more settings
        type                = "redis"
        queue-name          = "akka-scheduled-job"
        # redis settings
        redis-host-and-port = "localhost:6379"
        redis-password      = ""
    }

    # list of workers, fully qualified class names
    workers = [
        com.github.btnguyen2k.akkascheduledjob.samples.DummyWorker
    ]
}
```

```
## Akka System configurations
akka {
    ...Akka Actor System's configurations
}
```

**Standalone application**

Standalone application (in `.zip` format) can be built via command `sbt universal:packageBin`. The generated `.zip` file will be created under directory `target/universal`.

Unzip the package and start the application with: `sh conf/server-prod.sh start`

Stop the application with: `sh conf/server-prod.sh stop`

**Docker Image**

Docker image can be build in 2 ways:
- Build Docker files with `sbt docker:stage`, generated files are placed under directory `target/docker`
- Build Docker image and publish to local with `sbt docker:publishLocal`

See more: http://www.scala-sbt.org/sbt-native-packager/formats/docker.html
