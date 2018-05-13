lazy val akkaVersion = "2.5.4"

lazy val finatraVersion = "18.4.0"
lazy val guiceVersion   = "4.1.0"

lazy val commonDependencies = Seq(
  "com.typesafe.akka"          %% "akka-actor"          % akkaVersion,
  "com.typesafe.akka"          %% "akka-testkit"        % akkaVersion,
  "com.typesafe.akka"          %% "akka-stream"         % akkaVersion,
  "com.typesafe.akka"          %% "akka-stream-testkit" % akkaVersion,
  "com.typesafe.akka"          %% "akka-contrib"        % akkaVersion,
  "com.typesafe.akka"          %% "akka-testkit"        % akkaVersion,
  "org.scalactic"              %% "scalactic"           % "3.0.1",
  "org.scalatest"              %% "scalatest"           % "3.0.1",
  "org.mockito"                % "mockito-all"          % "1.10.19",
  "com.typesafe"               % "config"               % "1.3.1",
  "ch.qos.logback"             % "logback-classic"      % "1.1.7",
  "com.typesafe.scala-logging" %% "scala-logging"       % "3.5.0",
  "com.google.inject"          % "guice"                % guiceVersion,
  "com.google.inject"          % "guice"                % guiceVersion % Test,
  "com.twitter"                %% "finagle-http"        % finatraVersion,
  "com.twitter"                %% "finatra-http"        % finatraVersion,
  "com.twitter"                %% "inject-server"       % finatraVersion,
  "com.twitter"                %% "inject-app"          % finatraVersion,
  "com.twitter"                %% "inject-core"         % finatraVersion,
  "com.twitter"                %% "inject-modules"      % finatraVersion
)

lazy val acTestDependencies = Seq(
  "com.twitter"                  %% "finagle-http"   % finatraVersion % Test,
  "com.twitter"                  %% "finatra-http"   % finatraVersion % Test,
  "com.twitter"                  %% "inject-server"  % finatraVersion % Test,
  "com.twitter"                  %% "inject-app"     % finatraVersion % Test,
  "com.twitter"                  %% "inject-core"    % finatraVersion % Test,
  "com.twitter"                  %% "inject-modules" % finatraVersion % Test,
  "com.google.inject.extensions" % "guice-testlib"   % guiceVersion   % Test,
  "com.twitter"                  %% "finatra-http"   % finatraVersion % Test classifier "tests",
  "com.twitter"                  %% "inject-server"  % finatraVersion % Test classifier "tests",
  "com.twitter"                  %% "inject-app"     % finatraVersion % Test classifier "tests",
  "com.twitter"                  %% "inject-core"    % finatraVersion % Test classifier "tests",
  "com.twitter"                  %% "inject-modules" % finatraVersion % Test classifier "tests"
)

lazy val commonSettings = Seq(
  organization := "sentinel",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.6"
)

lazy val root = Project(id = "sentinel-camera", base = file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(web, client, `acceptance-tests`, `sentinel-protobuf`)

lazy val web = (project in file("web"))
  .settings(commonSettings ++ Seq(name := "sentinel-alertservice-web"))
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(`sentinel-protobuf`)

lazy val client = (project in file("client"))
  .settings(commonSettings ++ Seq(name := "sentinel-alertservice-client"))
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(`sentinel-protobuf`)

lazy val `acceptance-tests` = (project in file("acceptance-tests"))
  .settings(commonSettings ++ Seq(name := "sentinel-alertservice-acceptance-tests"))
  .settings(libraryDependencies ++= commonDependencies ++ acTestDependencies)
  .dependsOn(web, client)

lazy val `sentinel-protobuf` = (project in file("sentinel-protobuf"))
  .settings(commonSettings: _*)
  .settings(
    PB.targets in Compile += scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )
