ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.wisecube"
ThisBuild / scalaVersion := "2.12.14"

lazy val root = (project in file("."))
  .settings(
    name := "wisecube"
  )

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"

libraryDependencies += "org.apache.spark" %% "spark-mllib" % "3.2.1" % "provided"

libraryDependencies += ("org.apache.jena" % "jena-arq" % "3.17.0")
  .exclude("com.fasterxml.jackson.core", "jackson-databind")

libraryDependencies += ("org.scalatest" %% "scalatest" % "3.2.11" % "test")
  .exclude("org.scala-lang.modules", "scala-xml")

lazy val utils = (project in file("utils"))
  .settings(
    assembly / assemblyJarName := "wisecube.jar",
    // more settings here ...
  )

ThisBuild / assemblyShadeRules := Seq(
  ShadeRule.rename("org.apache.commons.logging.**" -> "shadelogging.@1").inAll
)


ThisBuild / assemblyMergeStrategy := {
  case PathList("shadelogging", xs @ _*) => MergeStrategy.first
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case _ => MergeStrategy.deduplicate
}