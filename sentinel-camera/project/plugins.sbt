logLevel := Level.Warn
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4" exclude("org.apache.maven", "maven-plugin-api"))
addSbtPlugin("org.bytedeco" % "sbt-javacv" % "1.15")