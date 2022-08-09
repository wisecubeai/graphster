package com.graphster.orpheus.config

import com.graphster.orpheus.config.types.{BooleanSeqField, ConfSeqField, DoubleSeqField, LongSeqField, MetadataField, StringSeqField}
import org.scalatest.funsuite.AnyFunSuite

class ConfPathSpec extends AnyFunSuite {
  test("basic traversal") {
    val config = Configuration(
      "a" -> MetadataField("a"),
      "b" -> MetadataField(Configuration(
        "ba" -> MetadataField("ba"),
        "bb" -> MetadataField(Configuration(
          "bba" -> MetadataField("bba")
        )))),
      "c" -> ConfSeqField(Seq(
        Configuration(
          "c1a" -> MetadataField("c1a"),
          "c1b" -> MetadataField("c1b"),
        ),
        Configuration(
          "c2a" -> MetadataField("c2a")
        ))))
    assertResult(config.getString("a"))((config / "a").value.value)
    assertResult(config.getConf("b").get("bb"))((config / "b" / "bb").value)
    assertResult(config.getConfSeq("c")(1).get("c2a"))((config / "c" / 1 / "c2a").value)
  }
}
