package com.wisecube.orpheus.config.table

import com.wisecube.orpheus.config.Configuration
import com.wisecube.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class LiteralValueConfSpec extends AnyFunSuite {
  test("basic creation") {
    val lvc = LongValueConf(LiteralValueConf.ValueKey, 1)
    val dvc = DoubleValueConf(LiteralValueConf.ValueKey, 2.0)
    val bvc = BooleanValueConf(LiteralValueConf.ValueKey, value = true)
    val svc = StringValueConf(LiteralValueConf.ValueKey, "XYZ")

    assertResult(lvc)(LongValueConf(1))
    assertResult(dvc)(DoubleValueConf(2.0))
    assertResult(bvc)(BooleanValueConf(value = true))
    assertResult(svc)(StringValueConf("XYZ"))
  }

  test("conversions") {
    val lvc = LongValueConf("Long", 1)
    val dvc = DoubleValueConf("Double", 2.0)
    val bvc = BooleanValueConf("Boolean", value = true)
    val svc = StringValueConf("String", "XYZ")

    Seq(lvc, dvc, bvc, svc).foreach {
      c =>
        val metadata = c.metadata
        val json = c.json
        val yaml = c.yaml
        val fromMetadata = LiteralValueConf.fromMetadata(metadata)
        val fromJson = LiteralValueConf(Configuration.loadJSONString(json))
        val fromYaml = LiteralValueConf(Configuration.loadYAMLString(yaml))
        assertResult(c, s"$c.name-metadata")(fromMetadata)
        assertResult(c, s"$c.name-json")(fromJson)
        assertResult(c, s"$c.name-yaml")(fromYaml)
    }
  }

  test("columns") {
    val lexp = 1
    val dexp = 2.0
    val bexp = true
    val sexp = "XYZ"
    val lvc = LongValueConf("Long", lexp)
    val dvc = DoubleValueConf("Double", dexp)
    val bvc = BooleanValueConf("Boolean", bexp)
    val svc = StringValueConf("String", sexp)
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(
      lvc.toColumn,
      dvc.toColumn,
      bvc.toColumn,
      svc.toColumn,
    )
    val first = df.first()
    assertResult(lexp)(first.getLong(1))
    assertResult(dexp)(first.getDouble(2))
    assertResult(bexp)(first.getBoolean(3))
    assertResult(sexp)(first.getString(4))
  }
}
