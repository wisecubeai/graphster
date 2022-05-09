resolvers += "Maven Central" at "https://repo1.maven.org/maven2/"

addDependencyTreePlugin

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.2.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.5")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")

/** scoverage */
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")