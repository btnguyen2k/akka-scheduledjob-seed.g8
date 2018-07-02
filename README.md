# akka-scheduledjob-seed.g8

Giter8 template for scheduled jobs using Akka in Java (see [ddth-akka](https://github.com/DDTH/ddth-akka/)).

To create a new project from template:

```
sbt new btnguyen2k/akka-scheduledjob-seed.g8
```

Latest release: [template-v0.2.0](RELEASE-NOTES.md).

## Features

- Build and package project with `sbt`:
  - `sbt eclipse`: generate Eclipse project
  - `sbt run`: run project (for development)
  - `sbt universal:packageBin`: package project as a `.zip` file
  - `sbt docker:publishLocal`: package project as docker image and publish to local
- Supported mode:
  - Single node
  - Multi-node (require Redis server)
  - Cluster (since [template-v0.2.0](RELEASE-NOTES.md)).
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
        jdbc-url      = ${JDBC_URL}
        jdbc-username = ${JDBC_USERNAME}
        jdbc-password = ${JDBC_PASSWORD}
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
    # Scheduling mode: "single-node", "multi-node" or "cluster"
    # If mode is "cluster", akka must run in cluster mode
    mode = "multi-node"

    # multi-node mode: distributed-lock time in milliseconds
    dlock-time-ms = 5000

    # multi-node mode: distributed-lock backend configurations
    dlock-backend {
        # either "local" or "redis"
        type                = "redis"
        lock-prefix         = ${app.shortname}
        lock-name           = "tick-fan-out"
        # redis settings
        redis-host-and-port = "localhost:6379"
        redis-password      = ""
    }

    # pub-sub backend configurations
    pubsub-backend {
        # either "local" or "redis"
        type                = "redis"
        channel-name        = ${app.shortname}
        # redis settings
        redis-host-and-port = "localhost:6379"
        redis-password      = ""
    }

    # List of workers, format: <fully-qualified-class-name>[;actor-name;dlock-name]
    # If actor-name or dlock-name is not supplied, use class' simple-name as actor-name & dlock-name
    workers = [
        com.github.btnguyen2k.akkascheduledjob.samples.TakeAllTasksWorker;taks-all-tasks
        com.github.btnguyen2k.akkascheduledjob.samples.LocalSingletonWorker
        com.github.btnguyen2k.akkascheduledjob.samples.GlobalSingletonWorker;global-singleton;global-singleton-lock
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
