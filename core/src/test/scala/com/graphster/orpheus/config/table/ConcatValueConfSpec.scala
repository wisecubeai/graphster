package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class ConcatValueConfSpec extends AnyFunSuite {
  test("basic creation") {
    val expValues = Seq(
      ColumnValueConf("sqr_ix", "ix*ix"),
      StringValueConf("-test")
    )
    val cvc = ConcatValueConf("test", expValues)
    assertResult(expValues)(cvc.values)
  }

  test("conversions") {
    val expValues = Seq(
      ColumnValueConf("sqr_ix", "ix*ix"),
      StringValueConf("-test")
    )
    val cvc = ConcatValueConf("test", expValues)
    val metadata = cvc.metadata
    val json = cvc.json
    val yaml = cvc.yaml
    val fromMetadata = ConcatValueConf.fromMetadata(metadata)
    val fromJson = ConcatValueConf(Configuration.loadJSONString(json))
    val fromYaml = ConcatValueConf(Configuration.loadYAMLString(yaml))
    assertResult(cvc, "metadata")(fromMetadata)
    assertResult(cvc, "json")(fromJson)
    assertResult(cvc, "yaml")(fromYaml)
  }

  test("columns") {
    val expValues = Seq(
      ColumnValueConf("sqr_ix", "ix*ix"),
      StringValueConf("-test")
    )
    val cvc = ConcatValueConf("test", expValues)
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(cvc.toColumn)
    assert(df.columns.contains("test"))
    val (ixs, values) = df.collect().map(r => (r.getInt(0), r.getString(1))).unzip
    assertResult(ixs.map(ix => (ix * ix) + "-test"))(values)
  }
}
