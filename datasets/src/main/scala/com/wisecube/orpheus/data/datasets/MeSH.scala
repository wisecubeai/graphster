package com.wisecube.orpheus.data.datasets

import com.wisecube.orpheus.config.graph.TripleGraphConf
import com.wisecube.orpheus.data.utils
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.io.File

object MeSH {
  val MeshURL: String = "https://nlmpubs.nlm.nih.gov/projects/mesh/rdf/mesh.nt"
  def download(filename: String = new File("mesh.nt").getAbsolutePath): String =
    utils.fileDownloader(MeshURL, filename, overwrite = false)
  def load(path: String = new File("mesh.nt").getAbsolutePath): DataFrame = {
    val spark = SparkSession.builder.getOrCreate()
    val lines = spark.read.text(path)
    lines.withColumn("triple", TripleGraphConf.triple2row(lines("value"))).selectExpr("triple.*")
  }
}
