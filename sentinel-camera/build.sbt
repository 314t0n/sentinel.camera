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
  "com.google.inject"          % "guice"                % "4.1.0"
)

lazy val commonSettings = Seq(
  organization := "sentinel",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.1",
  javaCppPresetLibs ++= Seq(
    "ffmpeg" -> "3.2.1"
  )
)

lazy val root = Project(id = "sentinel-camera", base = file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .aggregate(core, itest)
  .dependsOn(core, itest)
//  .settings(addArtifact(artifact in (Compile, assembly), assembly).settings: _*)

lazy val core = (project in file("core"))
  .settings(commonSettings ++ Seq(name := "sentinel-core"))
  .settings(libraryDependencies ++= commonDependencies)

lazy val itest = (project in file("integration-test"))
  .settings(
    commonSettings ++ Seq(name := "sentinel-integration-test")
  )
  .dependsOn(core)

//javaCppPlatform := Seq("linux-arm")
