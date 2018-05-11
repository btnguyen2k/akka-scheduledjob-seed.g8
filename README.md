# akka-scheduledjob-seed.g8

Giter8 template for scheduled jobs using Akka in Java (see [ddth-akka](https://github.com/DDTH/ddth-akka/)).

To create a new project from template:

```
sbt new btnguyen2k/akka-scheduledjob-seed.g8
```

Latest release: [template-v0.1.0](RELEASE-NOTES.md).

## Features

- Build and package project with `sbt`:
  - `sbt eclipse`: generate Eclipse project
  - `sbt run`: run project (for development)
  - `sbt universal:packageBin`: package project as a `.zip` file
  - `sbt docker:publishLocal`: package project as docker image and publish to local
- Support multi-node mode (require Redis server)
- Samples worker implementations (see [`com.github.btnguyen2k.akkascheduledjob.samples`](src/main/java/com/github/btnguyen2k/akkascheduledjob/samples)

## Configurations

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


## LICENSE & COPYRIGHT

See [LICENSE.md](LICENSE.md) for details. Copyright (c) 2018 Thanh Ba Nguyen.

Third party libraries are distributed under their own licenses.

## Giter8 template. 

For information on giter8 templates, please see http://www.foundweekends.org/giter8/
