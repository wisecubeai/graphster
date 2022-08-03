package com.wisecube.orpheus.config.graph

import com.wisecube.orpheus.config.Configuration
import com.wisecube.orpheus.config.table.{ColumnValueConf, ConcatValueConf, StringValueConf}
import com.wisecube.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class URIGraphConfSpec extends AnyFunSuite {
  test("basic creation") {
    val expStr = "http://www.example.com/abc"
    val expCol = StringValueConf(expStr)
    val uri = URIGraphConf("test", expCol)
    assertResult(expCol)(uri.uri)
    assertResult(uri)(URIGraphConf("test", expStr))
  }

  test("conversions") {
    val expStr = "http://www.example.com/abc"
    val expCol = StringValueConf(expStr)
    val uri = URIGraphConf("test", expCol)
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
    val uri = URIGraphConf("test", expCol)
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(uri.toColumn)
    assert(df.columns.contains("test"))
    val row = df.first()
    val nodeRow = NodeRow(row.getStruct(1))
    assertResult(expNodeRow)(nodeRow)
  }
}
