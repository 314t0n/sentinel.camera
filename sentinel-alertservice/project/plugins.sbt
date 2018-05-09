logLevel := Level.Warn

// Packaging

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4" exclude("org.apache.maven", "maven-plugin-api"))

// Scala CV

addSbtPlugin("org.bytedeco" % "sbt-javacv" % "1.15")

// ProtoBuf

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.18")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.7.1"