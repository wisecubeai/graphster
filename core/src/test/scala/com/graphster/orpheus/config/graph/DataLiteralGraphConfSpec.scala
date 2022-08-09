package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class DataLiteralGraphConfSpec extends AnyFunSuite {
  test("basic creation") {
    val expLex = "123"
    val expDt = "integer"
    val dlit = DataLiteralGraphConf("test", StringValueConf(expLex), StringValueConf(expDt))
    assertResult(StringValueConf(expLex))(dlit.lex)
    assertResult(StringValueConf(expDt))(dlit.datatype)
    assertResult(dlit)(DataLiteralGraphConf("test", expLex, expDt))
  }

  test("conversions") {
    val expLex = "123"
    val expDt = "integer"
    val dlit = DataLiteralGraphConf("test", StringValueConf(expLex), StringValueConf(expDt))
    val metadata = dlit.metadata
    val json = dlit.json
    val yaml = dlit.yaml
    val fromMetadata = DataLiteralGraphConf.fromMetadata(metadata)
    val fromJson = DataLiteralGraphConf(Configuration.loadJSONString(json))
    val fromYaml = DataLiteralGraphConf(Configuration.loadYAMLString(yaml))
    assertResult(dlit, "metadata")(fromMetadata)
    assertResult(dlit, "json")(fromJson)
    assertResult(dlit, "yaml")(fromYaml)
  }

  test("column") {
    val expLex = "123"
    val expDt = "integer"
    val expDtURI = DataLiteralGraphConf.xsdTypes(expDt).getURI
    val expNodeRow = NodeRow(
      DataLiteralGraphConf.NodeType,
      lex = expLex, datatype = expDtURI
    )
    val dlit = DataLiteralGraphConf("test", StringValueConf(expLex), StringValueConf(expDt))
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(dlit.toColumn)
    assert(df.columns.contains("test"))
    val row = df.first()
    val nodeRow = NodeRow(row.getStruct(1))
    assertResult(expNodeRow)(nodeRow)
  }
}
