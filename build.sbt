ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.graphster"
ThisBuild / scalaVersion := "2.12.14"

lazy val root = (project in file("."))
  .settings(
    name := "graphster"
  )

lazy val global = project
  .in(file("."))
  .settings(settings: _*)
  .disablePlugins(AssemblyPlugin)
  .aggregate(
    core,
    text,
    query,
    ai
  )

lazy val core = project
  .settings(
    moduleName := "graphster-core",
    assembly / assemblyDefaultJarName := s"${moduleName.value}_${version.value}.jar",
    settings,
    libraryDependencies ++= coreDependencies
  )

lazy val text = project
  .settings(
    moduleName := "graphster-text",
    assembly / assemblyDefaultJarName := s"${moduleName.value}_${version.value}.jar",
    settings,
    libraryDependencies ++= textDependencies
  )
  .dependsOn(core)

lazy val query = project
  .settings(
    moduleName := "graphster-query",
    assembly / assemblyDefaultJarName := s"${moduleName.value}_${version.value}.jar",
    settings,
    libraryDependencies ++= queryDependencies
  )
  .dependsOn(core)

lazy val ai = project
  .settings(
    moduleName := "graphster-ai",
    assembly / assemblyDefaultJarName := s"${moduleName.value}_${version.value}.jar",
    settings,
    libraryDependencies ++= aiDependenceis
  )
  .dependsOn(core)

lazy val datasets = project
  .settings(
    moduleName := "graphster-datasets",
    assembly / assemblyDefaultJarName := s"${moduleName.value}_${version.value}.jar",
    settings,
    libraryDependencies ++= datasetDependencies
  )
  .dependsOn(core)

lazy val textjsl = project
  .settings(
    moduleName := "graphster-text-jsl",
    assembly / assemblyDefaultJarName := s"${moduleName.value}_${version.value}.jar",
    settings,
    libraryDependencies ++= textJSLDependencies
  )
  .dependsOn(core, text)

lazy val demo = project
  .settings(
    moduleName := "graphster-demo",
    assembly / assemblyDefaultJarName := s"${moduleName.value}_${version.value}.jar",
    settings,
    libraryDependencies ++= (coreDependencies ++ datasetDependencies ++ queryDependencies).distinct
  )
  .dependsOn(core, datasets, query)

lazy val dependencies = new {
  val combinators = ("org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2")
  val spark = "org.apache.spark" %% "spark-mllib" % "3.2.1" % "provided"
  val jena = ("org.apache.jena" % "jena-arq" % "3.17.0")
    .exclude("com.fasterxml.jackson.core", "jackson-databind")
  val scalatest = ("org.scalatest" %% "scalatest" % "3.2.12" % "test")
    .exclude("org.scala-lang.modules", "scala-xml")
  val sparknlp = ("com.johnsnowlabs.nlp" %% "spark-nlp" % "3.4.4" % "provided")
  val xml = ("com.fasterxml.jackson.dataformat" % "jackson-dataformat-xml" % "2.12.3")
  val yaml = ("com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.12.3")
  val jacksonscala = ("com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.3")
  val jsoup = "org.jsoup" % "jsoup" % "1.15.2"
  val bellman = "com.github.gsk-aiops" %% "bellman-spark-engine" % "2.0.0"
}

lazy val coreDependencies = Seq(
  dependencies.combinators,
  dependencies.spark,
  dependencies.jena,
  dependencies.scalatest,
  dependencies.yaml,
  dependencies.xml,
  dependencies.jacksonscala,
)

lazy val textDependencies = Seq(
  dependencies.spark,
  dependencies.scalatest
)

lazy val textJSLDependencies = textDependencies :+ dependencies.sparknlp

lazy val queryDependencies = Seq(
  dependencies.spark,
  dependencies.jena,
  dependencies.bellman,
  dependencies.scalatest
)

val aiDependenceis = Seq(
  dependencies.spark,
  dependencies.scalatest
)

val datasetDependencies = Seq(
  dependencies.spark,
  dependencies.jena,
  dependencies.jsoup,
  dependencies.scalatest
)

lazy val settings = commonSettings ++ assemblySettings

lazy val commonSettings = Seq(
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  )
)

lazy val assemblySettings = Seq(
  ThisBuild / assemblyShadeRules := Seq(
    ShadeRule.rename("org.apache.commons.logging.**" -> "shadelogging.@1").inAll
  ),
  ThisBuild / assemblyMergeStrategy := {
    case PathList("shadelogging", xs @ _*) => MergeStrategy.first
    case x if Assembly.isConfigFile(x) =>
      MergeStrategy.concat
    case PathList(ps @ _*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
      MergeStrategy.rename
    case x if x.endsWith("module-info.class") => MergeStrategy.discard
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
)
