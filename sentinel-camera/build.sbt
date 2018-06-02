import org.bytedeco.sbt.javacv.Plugin.autoImport.javaCVVersion

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
  "com.twitter"                %% "finagle-http"        % "18.4.0"
)

lazy val commonSettings = Seq(
  organization := "sentinel",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.6",
  //  javaCVVersion := "1.3.2",
  javaCppPresetLibs ++= Seq(
    "ffmpeg" -> "3.2.1"
  )
)

lazy val root = Project(id = "sentinel-camera", base = file("."))
  .settings(commonSettings: _*)
  .settings(libraryDependencies ++= commonDependencies)
  .aggregate(core, `sentinel-protobuf`, `integration-test`)
  .dependsOn(core, `sentinel-protobuf`, `integration-test`)
//  .settings(addArtifact(artifact in (Compile, assembly), assembly).settings: _*)

lazy val core = (project in file("core"))
  .settings(commonSettings ++ Seq(name := "sentinel-core"))
  .settings(libraryDependencies ++= commonDependencies)

lazy val `sentinel-protobuf` = (project in file("sentinel-protobuf"))
  .settings(commonSettings: _*)
  .settings(
    PB.targets in Compile += scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )
//  .settings(exportJars := true) // scalapb might need it

lazy val `integration-test` = (project in file("integration-test"))
  .settings(
    commonSettings ++ Seq(name := "sentinel-integration-test")
  )
  .dependsOn(core)

javaCppPlatform := Seq("linux-armhf")
