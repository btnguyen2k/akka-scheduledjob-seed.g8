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
    fullname  = \${app.name} \${app.version}
    desc      = "Application description, free text"
}
```

```
## Datasource configurations
datasources {
    # Name of the datasource, in this case it's "default"
    default {
        jdbc-url      = \${JDBC_URL}
        jdbc-username = \${JDBC_USERNAME}
        jdbc-password = \${JDBC_PASSWORD}
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
        lock-prefix         = \${app.shortname}
        lock-name           = "tick-fan-out"
        # redis settings
        redis-host-and-port = "localhost:6379"
        redis-password      = ""
    }

    # pub-sub backend configurations
    pubsub-backend {
        # either "local" or "redis"
        type                = "redis"
        channel-name        = \${app.shortname}
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

**Standalone application**

Standalone application (in `.zip` format) can be built via command `sbt universal:packageBin`. The generated `.zip` file will be created under directory `target/universal`.

Unzip the package and start the application with: `sh conf/server-prod.sh start`

Stop the application with: `sh conf/server-prod.sh stop`

**Docker Image**

Docker image can be build in 2 ways:
- Build Docker files with `sbt docker:stage`, generated files are placed under directory `target/docker`.
Then docker image be build with command `docker build --force-rm --squash ./target/docker/stage`
- Build Docker image and publish to local with `sbt docker:publishLocal`

See more: http://www.scala-sbt.org/sbt-native-packager/formats/docker.html
