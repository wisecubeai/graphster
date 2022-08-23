package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.utils.SparkUtils
import org.apache.spark.sql.{functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class FallbackValueConfSpec extends AnyFunSuite {
  test("basic creation") {
    val expColumns = Seq(
      ColumnValueConf("ix*ix"),
      ColumnValueConf("ix+ix"),
    )
    val expFinalVal = StringValueConf("XYZ")
    val fvc = FallbackValueConf(expColumns, expFinalVal)
    assertResult(expColumns)(fvc.columns)
    assertResult(expFinalVal)(fvc.finalVal)
  }

  test("conversions") {
    val expColumns = Seq(
      ColumnValueConf("ix*ix"),
      ColumnValueConf("ix+ix"),
    )
    val expFinalVal = StringValueConf("XYZ")
    val fvc = FallbackValueConf(expColumns, expFinalVal)
    val metadata = fvc.metadata
    val json = fvc.json
    val yaml = fvc.yaml
    val fromMetadata = FallbackValueConf.fromMetadata(metadata)
    val fromJson = FallbackValueConf(Configuration.loadJSONString(json))
    val fromYaml = FallbackValueConf(Configuration.loadYAMLString(yaml))
    assertResult(fvc, "metadata")(fromMetadata)
    assertResult(fvc, "json")(fromJson)
    assertResult(fvc, "yaml")(fromYaml)
  }

  test("columns") {
    val expColumns = Seq(
      ColumnValueConf("IF(ix % 2 = 0, NULL, 'a')"),
      ColumnValueConf("IF(ix % 4 = 0, NULL, 'b')"),
    )
    val expFinalVal = StringValueConf("final")
    val fvc = FallbackValueConf(expColumns, expFinalVal)
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(50)(expColumns.map(_.toColumn): _*)
      .select(sf.expr("*"), fvc.toColumn)
    assert(df.columns.contains(fvc.name))
    val (ixs, values) = df.collect().map(r => (r.getInt(0), r.getString(3))).unzip
    val expValues = ixs.map {
      ix =>
        if (ix % 4 == 0) {
          "final"
        } else if (ix % 2 == 0) {
          "b"
        } else {
          "a"
        }
    }
    assertResult(expValues)(values)
  }
}
