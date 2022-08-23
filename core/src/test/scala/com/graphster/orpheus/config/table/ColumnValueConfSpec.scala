package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class ColumnValueConfSpec extends AnyFunSuite {

  test("basic creation") {
    val expr = "ix + 5"
    val cvc = ColumnValueConf(expr)

    assertResult(expr)(cvc.expression)
  }

  test("conversions") {
    val cvc = ColumnValueConf("ix + 5")
    val metadata = cvc.metadata
    val json = cvc.json
    val yaml = cvc.yaml
    val fromMetadata = ColumnValueConf.fromMetadata(metadata)
    val fromJson = ColumnValueConf(Configuration.loadJSONString(json))
    val fromYaml = ColumnValueConf(Configuration.loadYAMLString(yaml))
    assertResult(cvc, "metadata")(fromMetadata)
    assertResult(cvc, "json")(fromJson)
    assertResult(cvc, "yaml")(fromYaml)
  }

  test("columns") {
    val cvc = ColumnValueConf("ix + 5")
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(cvc.toColumn)
    assert(df.columns.contains(cvc.name))
    val (ixs, values) = df.collect().map(r => (r.getInt(0), r.getString(1))).unzip
    assertResult(ixs.map(ix => (ix + 5).toString))(values)
  }
}
