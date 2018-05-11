import com.typesafe.config._

val conf       = ConfigFactory.parseFile(new File("conf/application.conf")).resolve()
val appName    = conf.getString("app.name").toLowerCase().replaceAll("\\\\W+", "-")
val appVersion = conf.getString("app.version")

lazy val root = (project in file(".")).enablePlugins(JavaAppPackaging, DockerPlugin).settings(
    name         := appName,
    version      := appVersion,
    organization := "$organization$"
    //scriptedLaunchOpts ++= List("-Xms1024m", "-Xmx1024m", "-XX:ReservedCodeCacheSize=128m", "-XX:MaxPermSize=256m", "-Xss2m", "-Dfile.encoding=UTF-8"),
)

/*----------------------------------------------------------------------*/

fork := true

val _mainClass = "com.github.btnguyen2k.akkascheduledjob.Bootstrap"

/* Packaging options */
mainClass in (Compile, packageBin)       := Some(_mainClass)
sources in (Compile, doc)                := Seq.empty
publishArtifact in (Compile, packageDoc) := false
publishArtifact in (Compile, packageSrc) := false
autoScalaLibrary                         := false
// add conf/ directory
mappings in Universal                    ++= (baseDirectory.value / "conf" * "*" get) map(x => x -> ("conf/" + x.getName))

/* Docker packaging options */
dockerCommands := Seq()
import com.typesafe.sbt.packager.docker._
dockerCommands := Seq(
    Cmd("FROM", "openjdk:8-jre-alpine"),
    Cmd("LABEL", "maintainer=\"$app_author$\""),
    Cmd("ADD", "opt /opt"),
    Cmd("RUN", "apk add --no-cache bash && ln -s /opt/docker /opt/" + appName + " && chown -R daemon:daemon /opt"),	
    Cmd("WORKDIR", "/opt/" + appName),
    Cmd("USER", "daemon"),
    ExecCmd("ENTRYPOINT", "./conf/server-docker.sh", "start")
    //Cmd("CMD", "./conf/server-docker.sh start")
)
packageName in Docker                 := appName
version in Docker                     := appVersion

/* Compiling  options */
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

/* Run options */
javaOptions  ++= collection.JavaConverters.propertiesAsScalaMap(System.getProperties)
    .map{ case (key,value) => "-D" + key + "=" +value }.toSeq
mainClass in (Compile, run) := Some(_mainClass)

/* Eclipse settings */
EclipseKeys.projectFlavor                := EclipseProjectFlavor.Java                   // Java project. Don't expect Scala IDE
EclipseKeys.executionEnvironment         := Some(EclipseExecutionEnvironment.JavaSE18)  // expect Java 1.8

/* Dependencies */
val _slf4jVersion       = "1.7.25"
val _ddthCommonsVersion = "0.9.1.3"
val _ddthAkkaVersion    = "0.1.0.1"
val _ddthDlockVersion   = "0.1.1.2"
val _ddthQueueVersion   = "0.6.2.6"
val _akkaVersion        = "2.5.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-actor"                   % _akkaVersion

  ,"org.slf4j"                  % "slf4j-api"                    % _slf4jVersion
  ,"org.slf4j"                  % "log4j-over-slf4j"             % _slf4jVersion
  ,"ch.qos.logback"             % "logback-classic"              % "1.2.3"

  ,"org.apache.commons"         % "commons-lang3"                % "3.7"
  ,"io.undertow"                % "undertow-core"                % "2.0.6.Final"

  ,"com.github.ddth"            % "ddth-dlock-core"              % _ddthDlockVersion
  ,"com.github.ddth"            % "ddth-dlock-redis"             % _ddthDlockVersion
  ,"com.github.ddth"            % "ddth-queue-core"              % _ddthQueueVersion
  ,"com.github.ddth"            % "ddth-queue-redis"             % _ddthQueueVersion
  ,"com.github.ddth"            % "ddth-akka-core"               % _ddthAkkaVersion
  ,"com.github.ddth"            % "ddth-commons-core"            % _ddthCommonsVersion
  ,"com.github.ddth"            % "ddth-commons-typesafeconfig"  % _ddthCommonsVersion
)
