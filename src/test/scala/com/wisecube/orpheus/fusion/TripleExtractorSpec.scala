package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.graph._
import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.{Row, functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class TripleExtractorSpec extends AnyFunSuite {
  test("testing basic processing") {
    val tripleMarkers = Array(
      new TripleMarker().setInputCol("uri1").setTripleMeta(TripleElement.Meta(
        "index_triples",
        URIElement.Meta("subject", ColumnValueMeta("uri1")),
        URIElement.Meta("predicate", "http://www.example.com/index"),
        DataLiteralElement.Meta("index", ColumnValueMeta("ix"), "integer")
      )),
      new TripleMarker().setInputCol("uri1").setTripleMeta(TripleElement.Meta(
        "first_rel",
        URIElement.Meta("subject", ColumnValueMeta("uri1")),
        URIElement.Meta("predicate", "http://www.example.com/rel12"),
        URIElement.Meta("object", ColumnValueMeta("uri2")),
      )),
      new TripleMarker().setInputCol("uri2").setTripleMeta(TripleElement.Meta(
        "third_rel",
        URIElement.Meta("subject", ColumnValueMeta("uri2")),
        URIElement.Meta("predicate", "http://www.example.com/rel2b"),
        BlankElement.Meta("object", ColumnValueMeta("ix")),
      )),
      new TripleMarker().setInputCol("uri3").setTripleMeta(TripleElement.Meta(
        "fourth_rel",
        BlankElement.Meta("subject", ColumnValueMeta("ix")),
        URIElement.Meta("predicate", "http://www.example.com/relb3"),
        URIElement.Meta("object", ColumnValueMeta("uri3")),
      )),
    )
    val pipeline = new Pipeline().setStages(tripleMarkers :+ new TripleExtractor())
    val df = SparkUtils.generateDataFrame(10)(
      sf.concat(sf.lit("http://www.example.com/abc/"), sf.format_string("%.0f", sf.rand() * 10000)).as("uri1"),
      sf.concat(sf.lit("http://www.example.com/xyz/"), sf.format_string("%.0f", sf.rand() * 10000)).as("uri2"),
      sf.concat(sf.lit("http://www.example.com/rst/"), sf.format_string("%.0f", sf.rand() * 10000)).as("uri3")
    )
    val linesExp = df.collect().flatMap {
      case Row(ix: Int, uri1: String, uri2: String, uri3: String) =>
        val uri1Node = URIElement.parts2jena(uri1)
        val uri2Node = URIElement.parts2jena(uri2)
        val uri3Node = URIElement.parts2jena(uri3)
        val blankNode = BlankElement.parts2jena(ix.toString)
        val indexNode = DataLiteralElement.parts2jena(ix.toString, "integer")
        val indexPred = URIElement.parts2jena("http://www.example.com/index")
        val pred12 = URIElement.parts2jena("http://www.example.com/rel12")
        val pred2b = URIElement.parts2jena("http://www.example.com/rel2b")
        val predb3 = URIElement.parts2jena("http://www.example.com/relb3")
        Array(
          TripleElement.parts2jena(uri1Node, indexPred, indexNode),
          TripleElement.parts2jena(uri1Node, pred12, uri2Node),
          TripleElement.parts2jena(uri2Node, pred2b, blankNode),
          TripleElement.parts2jena(blankNode, predb3, uri3Node)
        ).map(TripleElement.jena2string)
    }.toSet
    val tx = pipeline.fit(df).transform(df)
    val linesObs = tx.select(
      TripleElement.row2triple(sf.struct(TripleElement.subjectKey, TripleElement.predicateKey, TripleElement.objectKey))
    ).collect().map(_.getString(0)).toSet
    assertResult(linesExp)(linesObs)
  }

}
