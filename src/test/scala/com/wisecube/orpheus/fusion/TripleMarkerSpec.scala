package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.SparkImplicits._
import com.wisecube.orpheus.graph.{ColumnValueMeta, DataLiteralElement, TripleElement, URIElement}
import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.{functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class TripleMarkerSpec extends AnyFunSuite {

  test("testing basic processing") {
    val tripleMarker = new TripleMarker().setInputCol("subject").setTripleMeta(
      TripleElement.Meta(
        "index_triples",
        URIElement.Meta("subject", ColumnValueMeta("subject")),
        URIElement.Meta("predicate", "http://www.example.com/index"),
        DataLiteralElement.Meta("index", ColumnValueMeta("ix"), "integer")
      ))
    val pipeline = new Pipeline().setStages(Array(tripleMarker))
    val df = SparkUtils.generateDataFrame(10)(
      sf.expr(s"CONCAT('http://www.', CAST(ix AS string), '.com/', CAST(ix*ix AS string), '/abc')").as("subject")
    )
    val tx = pipeline.fit(df).transform(df)
    val fieldOpt = tx.schema.find(_.name == tripleMarker.getInputCol)
    assert(fieldOpt.nonEmpty)
    val field = fieldOpt.get
    val allTriples = field.metadata.getMetadataArrayOrElse(TriplesMetadataKey)
    assert(allTriples.nonEmpty)
    assert(allTriples.contains(tripleMarker.getTripleMeta.toMetadata))
  }
}
