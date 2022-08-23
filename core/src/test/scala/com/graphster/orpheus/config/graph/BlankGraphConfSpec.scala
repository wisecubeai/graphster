package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class BlankGraphConfSpec extends AnyFunSuite {
  test("basic creation") {
    val expId = "abc"
    val blank = BlankGraphConf(StringValueConf(expId))
    assertResult(StringValueConf(expId))(blank.blankId)
    assertResult(blank)(BlankGraphConf(expId))
  }

  test("conversions") {
    val expId = "abc"
    val blank = BlankGraphConf(StringValueConf(expId))
    val metadata = blank.metadata
    val json = blank.json
    val yaml = blank.yaml
    val fromMetadata = BlankGraphConf.fromMetadata(metadata)
    val fromJson = BlankGraphConf(Configuration.loadJSONString(json))
    val fromYaml = BlankGraphConf(Configuration.loadYAMLString(yaml))
    assertResult(blank, "metadata")(fromMetadata)
    assertResult(blank, "json")(fromJson)
    assertResult(blank, "yaml")(fromYaml)
  }

  test("column") {
    val expId = "abc"
    val expNodeRow = NodeRow(
      BlankGraphConf.NodeType,
      blankId = expId
    )
    val blank = BlankGraphConf(StringValueConf(expId))
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(blank.toColumn)
    assert(df.columns.contains(blank.name))
    val row = df.first()
    val nodeRow = NodeRow(row.getStruct(1))
    assertResult(expNodeRow)(nodeRow)
  }
}
