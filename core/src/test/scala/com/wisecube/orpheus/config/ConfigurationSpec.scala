package com.wisecube.orpheus.config

import com.wisecube.orpheus.config.types._
import org.scalatest.funsuite.AnyFunSuite

class ConfigurationSpec extends AnyFunSuite {
  test("basic creation") {
    val fields: Map[String, MetadataField[_]] = Map(
      "a" -> MetadataField(1),
      "b" -> LongSeqField(Array(1, 2)),
      "c" -> MetadataField(2.0),
      "d" -> DoubleSeqField(Array(2.0, 3.0)),
      "e" -> MetadataField(true),
      "f" -> BooleanSeqField(Array(true, false)),
      "g" -> MetadataField("XYZ"),
      "h" -> StringSeqField(Array("XYZ", "ABC")),
      "i" -> MetadataField(Configuration.empty),
      "j" -> ConfSeqField(Array(Configuration.empty)),
    )
    val config = Configuration(
      "a" -> MetadataField(1),
      "b" -> LongSeqField(Array(1, 2)),
      "c" -> MetadataField(2.0),
      "d" -> DoubleSeqField(Array(2.0, 3.0)),
      "e" -> MetadataField(true),
      "f" -> BooleanSeqField(Array(true, false)),
      "g" -> MetadataField("XYZ"),
      "h" -> StringSeqField(Array("XYZ", "ABC")),
      "i" -> MetadataField(Configuration.empty),
      "j" -> ConfSeqField(Array(Configuration.empty)),
    )
    assertResult(fields.keySet)(config.keys)
    fields.foreach {
      case (key, value) =>
        assertResult(value, key)(config.get(key))
    }
  }

  test("conversions") {
    val config = Configuration(
      "a" -> MetadataField(1),
      "b" -> LongSeqField(Array(1, 2)),
      "c" -> MetadataField(2.0),
      "d" -> DoubleSeqField(Array(2.0, 3.0)),
      "e" -> MetadataField(true),
      "f" -> BooleanSeqField(Array(true, false)),
      "g" -> MetadataField("XYZ"),
      "h" -> StringSeqField(Array("XYZ", "ABC")),
      "i" -> MetadataField(Configuration.empty),
      "j" -> ConfSeqField(Array(Configuration.empty)),
    )
    val metadata = config.metadata
    val json = config.json
    val yaml = config.yaml
    val fromMetadata = Configuration.fromMetadata(metadata)
    val fromJson = Configuration.loadJSONString(json)
    val fromYaml = Configuration.loadYAMLString(yaml)
    assertResult(config, "metadata")(fromMetadata)
    assertResult(config, "json")(fromJson)
    assertResult(config, "yaml")(fromYaml)
  }
}
