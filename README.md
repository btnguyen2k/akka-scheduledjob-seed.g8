# akka-scheduledjob-seed.g8

Giter8 template for scheduled jobs using Akka in Java (see [ddth-akka](https://github.com/DDTH/ddth-akka/)).

To create a new project from template:

```
sbt new btnguyen2k/akka-scheduledjob-seed.g8
```

Latest release: [template-v0.1.2.1](RELEASE-NOTES.md).

## Features

- Build and package project with `sbt`:
  - `sbt eclipse`: generate Eclipse project
  - `sbt run`: run project (for development)
  - `sbt universal:packageBin`: package project as a `.zip` file
  - `sbt docker:publishLocal`: package project as docker image and publish to local
- Support multi-node mode (require Redis server)
- Bootstrappers (since [template-v0.1.1](RELEASE-NOTES.md)).
- Built-in bootstrapper to bootstrap `javax.sql.DataSource` (see [`com.github.btnguyen2k.akkascheduledjob.bootstrap.DataSourcesBootstrapper`](src/main/java/com/github/btnguyen2k/akkascheduledjob/bootstrap/DataSourcesBootstrapper.java)
- Samples worker implementations (see [`com.github.btnguyen2k.akkascheduledjob.samples`](src/main/java/com/github/btnguyen2k/akkascheduledjob/samples)

## Configurations

Application's main configuration file `conf/application.conf` in [HOCON format](https://github.com/lightbend/config/blob/master/HOCON.md).

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
## Declare bootstrappers to perform application's initializing tasks.
## Bootstrapper must implement Runnable interface.
bootstrappers = [
    com.github.btnguyen2k.akkascheduledjob.bootstrap.DataSourcesBootstrapper
]
```

```
## Datasource configurations
datasources {
    # Name of the datasource, in this case it's "default"
    default {
        jdbc-url      = "${JDBC_URL}"
        jdbc-username = "${JDBC_USERNAME}"
        jdbc-password = "${JDBC_PASSWORD}"
    }
    # Another datasource
    my-log-datasource {
        jdbc-url      = "jdbc:mysql://localhost:3306/test"
        jdbc-username = "test"
        jdbc-password = "test"
    }
}
```

```
## ddth-akka-scheduling configurations
ddth-akka-scheduling {
    # set to "true" to enable multi-mode, "false" otherwise
    multi-node-mode     = true

    # multi-node mode: d-lock time in milliseconds
    dlock-time-ms       = 5000

    # used in multi-mode only
    dlock-backend {
        # either "local" or "redis"
        type                = "redis"
        lock-prefix         = ${app.shortname}
        lock-name           = "tick-fan-out"
        # redis settings
        redis-host-and-port = "localhost:6379"
        redis-password      = ""
    }

    # used in multi-mode only
    pubsub-backend {
        # either "local" or "redis"
        type                = "redis"
        channel-name        = ${app.shortname}
        # redis settings
        redis-host-and-port = "localhost:6379"
        redis-password      = ""
    }

    # list of workers, format: <fully-qualified-class-name>[;actor-name;dlock-name;dlock-time-ms]
    workers = [
        com.github.btnguyen2k.akkascheduledjob.samples.TakeAllTasksWorker;TakeAllTasks
        com.github.btnguyen2k.akkascheduledjob.samples.LocalSingletonWorker;LocalSingletonWorker
        com.github.btnguyen2k.akkascheduledjob.samples.GlobalSingletonWorker;GlobalSingletonWorker;GlobalSingletonWorker;5000
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
