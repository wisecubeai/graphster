package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.sql.types.MetadataBuilder
import org.apache.spark.sql.{functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class LiteralValueMetaSpec extends AnyFunSuite {
  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testVal: String = "abc"
    val metaExp = LiteralValueMeta(testName, testVal)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, LiteralValueMeta.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putString(LiteralValueMeta.valueKey, testVal)
      .build()
    val metaObs = LiteralValueMeta.fromMetadata(metadataExp)
    val metaObs2 = LiteralValueMeta.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)

    assertThrows[IllegalArgumentException] {
      LiteralValueMeta("", "")
    }
  }

  test("Testing creation of columns") {
    val spark = SparkUtils.spark

    val testName = "name"
    val testVal: String = "abc"
    val meta = LiteralValueMeta(testName, testVal)
    val columnExp = sf.lit(testVal).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    assertResult(metadataExp)(metadataObs)
  }
}
