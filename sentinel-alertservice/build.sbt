lazy val akkaVersion = "2.5.4"

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
  "com.google.inject"          % "guice"                % "4.1.0",
  "com.twitter"                %% "finagle-http"        % "18.4.0",
  "com.twitter"                %% "finatra-http"        % "18.4.0"
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
  .settings(libraryDependencies ++= commonDependencies)
  .dependsOn(web, client)

lazy val `sentinel-protobuf` = (project in file("sentinel-protobuf"))
  .settings(commonSettings: _*)
  .settings(
    PB.targets in Compile += scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )
