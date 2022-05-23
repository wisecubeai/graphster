package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.graph._
import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.SQLTransformer
import org.apache.spark.sql.{functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class NodeExtractorSpec extends AnyFunSuite {
  test("testing basic processing") {
    SparkUtils.spark
    val nodeMarkers = Array(
      new NodeMarker().setInputCol("uri").setNodeMeta(
        URIElement.Meta("entity", ColumnValueMeta("uri"))),
      new NodeMarker().setInputCol("ix").setNodeMeta(
        DataLiteralElement.Meta("ixProp", ColumnValueMeta("ix"), LiteralValueMeta("integer"))),
      new NodeMarker().setInputCol("day").setNodeMeta(
        LangLiteralElement.Meta("dayProp", ColumnValueMeta("day"), LiteralValueMeta("en"))),
      new NodeMarker().setInputCol("day").setNodeMeta(
        LangLiteralElement.Meta("dayProp", ColumnValueMeta("day"), LiteralValueMeta("en-gb"))),
      new NodeMarker().setInputCol("id").setNodeMeta(
        BlankElement.Meta("blank", ColumnValueMeta("blankid", "id"))),
    )
    val pipeline = new Pipeline().setStages(
      new SQLTransformer().setStatement(
        """
          |SELECT
          |  ix, sq,
          |  CONCAT('http://www.example', CAST(ix AS string), '.com/', CAST(sq AS string)) AS uri,
          |  DATE_FORMAT(TO_DATE(CONCAT('2022-05-', CAST(((ix % 30) + 1) AS string)), 'yyyy-MM-dd'), 'E') AS day,
          |  CONCAT_WS('_', CAST(ix AS string), CAST(sq AS string)) AS id
          |  FROM __THIS__
          |""".stripMargin) +: nodeMarkers :+ new NodeExtractor()
    )
    val df = SparkUtils.generateDataFrame(10)(sf.pow("ix", 2).as("sq"))
    val tx = pipeline.fit(df).transform(df)
    nodeMarkers.foreach {
      nm =>
        val nodeName = nm.getNodeMeta.name
        val nodeFieldOpt = tx.schema.find(_.name == nodeName)
        assert(nodeFieldOpt.nonEmpty)
        val nodeField = nodeFieldOpt.get
        nodeField.dataType == NodeElement.schema
    }
  }

}
