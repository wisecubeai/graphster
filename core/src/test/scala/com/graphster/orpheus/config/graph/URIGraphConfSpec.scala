package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class URIGraphConfSpec extends AnyFunSuite {
  test("basic creation") {
    val expStr = "http://www.example.com/abc"
    val expCol = StringValueConf(expStr)
    val uri = URIGraphConf(expCol)
    assertResult(expCol)(uri.uri)
    assertResult(uri)(URIGraphConf(expStr))
  }

  test("conversions") {
    val expStr = "http://www.example.com/abc"
    val expCol = StringValueConf(expStr)
    val uri = URIGraphConf(expCol)
    val metadata = uri.metadata
    val json = uri.json
    val yaml = uri.yaml
    val fromMetadata = URIGraphConf.fromMetadata(metadata)
    val fromJson = URIGraphConf(Configuration.loadJSONString(json))
    val fromYaml = URIGraphConf(Configuration.loadYAMLString(yaml))
    assertResult(uri, "metadata")(fromMetadata)
    assertResult(uri, "json")(fromJson)
    assertResult(uri, "yaml")(fromYaml)
  }

  test("column") {
    val expNS = "http://www.example.com/"
    val expLocal = "abc"
    val expStr = expNS + expLocal
    val expCol = StringValueConf(expStr)
    val expNodeRow = NodeRow(
      URIGraphConf.NodeType,
      expStr, expNS, expLocal
    )
    val uri = URIGraphConf(expCol)
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(uri.toColumn)
    assert(df.columns.contains(uri.name))
    val row = df.first()
    val nodeRow = NodeRow(row.getStruct(1))
    assertResult(expNodeRow)(nodeRow)
  }
}
