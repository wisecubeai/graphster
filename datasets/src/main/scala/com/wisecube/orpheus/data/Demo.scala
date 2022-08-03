package com.wisecube.orpheus.data

import com.wisecube.orpheus.config.Configuration
import com.wisecube.orpheus.config.graph.{LangLiteralGraphConf, URIGraphConf}
import com.wisecube.orpheus.config.table.{ColumnValueConf, ConcatValueConf, FallbackValueConf, StringValueConf}
import com.wisecube.orpheus.config.types.MetadataField
import com.wisecube.orpheus.data.datasets.{ClinicalTrials, MeSH}
import com.wisecube.orpheus.enrichment.GraphLinker
import com.wisecube.orpheus.fusion.{TripleExtractor, TripleMarker}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.SQLTransformer
import org.apache.spark.sql.SparkSession

import java.nio.file.{Files, Paths}
import scala.language.postfixOps

object Demo {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val spark: SparkSession = SparkSession
      .builder()
      .appName("graphster-demo")
      .master("local[*]")
      .getOrCreate()

    val meshPath = Paths.get("data/mesh_table.parquet")
    if (!Files.isDirectory(meshPath)) {
      val filepath: String = MeSH.download()
      val meshDF = MeSH.load(filepath)

      meshDF.write.parquet(meshPath.toFile.getAbsolutePath)
    }

    val aactPath = Paths.get("data/aact")
    if (!Files.isDirectory(aactPath)) {
      val zipfilePath = ClinicalTrials.download()
      val directory = ClinicalTrials.unzip(zipfilePath)

      ClinicalTrials.getFiles(directory).foreach(println)
      ClinicalTrials.getFiles(directory).foreach {
        filename =>
          val tablename = filename.replace(".txt", "")
          val df = ClinicalTrials.load(filename, directory)
          val path = Paths.get(aactPath.toString, tablename + ".parquet")
          if (!path.toFile.isDirectory) {
            df.write.parquet(path.toFile.getAbsolutePath)
          }
      }
    }

    spark.read.parquet(meshPath.toFile.getAbsolutePath).createOrReplaceTempView("mesh")
    spark.read.parquet(
      Paths.get(aactPath.toString, "conditions" + ".parquet")
        .toFile.getAbsolutePath
    ).createOrReplaceTempView("conditions")
    spark.read.parquet(
      Paths.get(aactPath.toString, "interventions" + ".parquet")
        .toFile.getAbsolutePath
    ).createOrReplaceTempView("interventions")
    spark.read.parquet(
      Paths.get(aactPath.toString, "central_contacts" + ".parquet")
        .toFile.getAbsolutePath
    ).createOrReplaceTempView("central_contacts")

    println("mesh triples", spark.table("mesh").count())
    println("NCT conditions", spark.table("conditions").count())
    println("NCT interventions", spark.table("interventions").count())
    println("NCT central_contacts", spark.table("central_contacts").count())

    val config = Configuration(
      "mesh" -> MetadataField(Configuration(
        "label_uri" -> MetadataField(URIGraphConf("rdfs_label", "http://www.w3.org/2000/01/rdf-schema#label"))
      )),
      "nct" -> MetadataField(Configuration(
        "nct_uri" -> MetadataField(URIGraphConf("uri", ConcatValueConf("parts", Seq(
          StringValueConf("ns", "http://clinicaltrials.gov/ct2/show/"),
          ColumnValueConf("col", "nct_id")
        )))),
        "name" -> MetadataField(LangLiteralGraphConf("name", ColumnValueConf("name", "name"), StringValueConf("en"))),
        "condition" -> MetadataField(Configuration(
          "predicate" -> MetadataField(URIGraphConf("predicate", "http://schema.org/healthCondition")),
          "uri" -> MetadataField(URIGraphConf("uri", FallbackValueConf("condition",
            Seq(ColumnValueConf("meshid", "meshid")),
            ConcatValueConf(
              "uri", Seq(
                StringValueConf("ns", "http://clinicaltrials.gov/ct2/show/condition/"),
                ColumnValueConf("id", "id"),
              ))))))),
        "intervention" -> MetadataField(Configuration(
          "predicate" -> MetadataField(URIGraphConf("predicate", "http://schema.org/studySubject")),
          "uri" -> MetadataField(URIGraphConf("uri", FallbackValueConf("intervention",
            Seq(ColumnValueConf("meshid", "meshid")),
            ConcatValueConf(
              "uri", Seq(
                StringValueConf("ns", "http://clinicaltrials.gov/ct2/show/intervention/"),
                ColumnValueConf("id", "id"),
              )))))))
      )),
      "types" -> MetadataField(Configuration(
        "predicate" -> MetadataField(URIGraphConf("rdf_type", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")),
        "trial" -> MetadataField(URIGraphConf("medical_trial", "http://schema.org/MedicalTrial")),
        "condition" -> MetadataField(URIGraphConf("medical_condition", "https://schema.org/MedicalCondition")),
        "intervention" -> MetadataField(URIGraphConf("medical_procedure", "https://schema.org/MedicalProcedure")),
      ))
    )

    println(config.yaml)

    val conditionsDF = spark.table("conditions")
    val interventionsDF = spark.table("interventions")

    val linker = new GraphLinker()
      .setPropertyCol(config / "nct" / "name" / "lex" / "column" getString)
      .setPredicateCol(config / "mesh" / "label_uri" / "uri" / "value" getString)
      .setReferenceTable("mesh")
      .setTransformation("LOWER($)")
      .setJoinType("leftouter")
      .setOutputCol("meshid")

    val nctType = new TripleMarker()
      .setInputCol("nct_id")
      .setTripleMeta(
        "nct_type",
        config / "nct" / "nct_uri" getConf,
        config / "types" / "predicate" getConf,
        config / "types" / "trial" getConf
      )

    val removeUnamed = new SQLTransformer().setStatement(
      """
        |SELECT *
        |FROM __THIS__
        |WHERE name IS NOT NULL
        |""".stripMargin)

    val condType = new TripleMarker()
      .setInputCol(config / "nct" / "name" / "lex" / "column" getString)
      .setTripleMeta(
        "condition_type",
        config / "nct" / "condition" / "uri" getConf,
        config / "types" / "predicate" getConf,
        config / "types" / "condition" getConf
      )

    val condLabel = new TripleMarker()
      .setInputCol(config / "nct" / "name" / "lex" / "column" getString)
      .setTripleMeta(
        "condition_label",
        config / "nct" / "condition" / "uri" getConf,
        config / "mesh" / "label_uri" getConf,
        config / "nct" / "name" getConf
      )

    val condPred = new TripleMarker()
      .setInputCol("nct_id")
      .setTripleMeta(
        "condition",
        config / "nct" / "nct_uri" getConf,
        config / "nct" / "condition" / "predicate" getConf,
        config / "nct" / "condition" / "uri" getConf,
      )

    val conditionPipeline = new Pipeline().setStages(Array(
      linker,
      nctType,
      removeUnamed,
      condPred,
      condType,
      condLabel,
      new TripleExtractor()
    )).fit(conditionsDF)

    val intervType = new TripleMarker()
      .setInputCol(config / "nct" / "name" / "lex" / "column" getString)
      .setTripleMeta(
        "intervention_type",
        config / "nct" / "intervention" / "uri" getConf,
        config / "types" / "predicate" getConf,
        config / "types" / "intervention" getConf
      )

    val intervLabel = new TripleMarker()
      .setInputCol(config / "nct" / "name" / "lex" / "column" getString)
      .setTripleMeta(
        "intervention_label",
        config / "nct" / "intervention" / "uri" getConf,
        config / "mesh" / "label_uri" getConf,
        config / "nct" / "name" getConf
      )

    val intervPred = new TripleMarker()
      .setInputCol("nct_id")
      .setTripleMeta(
        "condition",
        config / "nct" / "nct_uri" getConf,
        config / "nct" / "intervention" / "predicate" getConf,
        config / "nct" / "intervention" / "uri" getConf,
      )

    val interventionPipeline = new Pipeline().setStages(Array(
      linker,
      nctType,
      removeUnamed,
      intervPred,
      intervType,
      intervLabel,
      new TripleExtractor()
    )).fit(interventionsDF)

    val conditionTriples = conditionPipeline.transform(conditionsDF).distinct()

    val interventionTriples = interventionPipeline.transform(interventionsDF).distinct()

    val nctTriples = conditionTriples.unionAll(interventionTriples).distinct()

    println("Number of NCT triples", nctTriples.count())

    val graph = spark.table("mesh").unionAll(nctTriples).distinct()

    println("Number of total triples", graph.count())

  }
}
