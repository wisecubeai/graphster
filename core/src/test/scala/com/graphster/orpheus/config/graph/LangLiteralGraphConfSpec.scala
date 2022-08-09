package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.utils.SparkUtils
import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.utils.SparkUtils
import org.scalatest.funsuite.AnyFunSuite

class LangLiteralGraphConfSpec extends AnyFunSuite {
  test("basic creation") {
    val expLex = "abc"
    val expLang = "en"
    val llit = LangLiteralGraphConf("test", StringValueConf(expLex), StringValueConf(expLang))
    assertResult(StringValueConf(expLex))(llit.lex)
    assertResult(StringValueConf(expLang))(llit.language)
    assertResult(llit)(LangLiteralGraphConf("test", expLex, expLang))
  }

  test("conversions") {
    val expLex = "abc"
    val expLang = "en"
    val llit = LangLiteralGraphConf("test", StringValueConf(expLex), StringValueConf(expLang))
    val metadata = llit.metadata
    val json = llit.json
    val yaml = llit.yaml
    val fromMetadata = LangLiteralGraphConf.fromMetadata(metadata)
    val fromJson = LangLiteralGraphConf(Configuration.loadJSONString(json))
    val fromYaml = LangLiteralGraphConf(Configuration.loadYAMLString(yaml))
    assertResult(llit, "metadata")(fromMetadata)
    assertResult(llit, "json")(fromJson)
    assertResult(llit, "yaml")(fromYaml)
  }

  test("column") {
    val expLex = "abc"
    val expLang = "en"
    val expNodeRow = NodeRow(
      LangLiteralGraphConf.NodeType,
      lex = expLex, language = expLang
    )
    val llit = LangLiteralGraphConf("test", StringValueConf(expLex), StringValueConf(expLang))
    val spark = SparkUtils.spark
    val df = SparkUtils.generateDataFrame(10)(llit.toColumn)
    assert(df.columns.contains("test"))
    val row = df.first()
    val nodeRow = NodeRow(row.getStruct(1))
    assertResult(expNodeRow)(nodeRow)
  }
}
