package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.sql.types.MetadataBuilder
import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.{functions => sf}

class ConcatValueMetaSpec extends AnyFunSuite {

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testValues = Seq(
      ColumnValueMeta("a"),
      LiteralValueMeta("b", "/abc"),
    )
    val metaExp = ConcatValueMeta(testName, testValues)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, ConcatValueMeta.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putMetadataArray(ConcatValueMeta.valuesKey, testValues.map(_.toMetadata).toArray)
      .build()
    val metaObs = ConcatValueMeta.fromMetadata(metadataExp)
    val metaObs2 = ConcatValueMeta.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    val testName = "name"
    val testCols = Seq("a", "b")
    val testValues = testCols.map(ColumnValueMeta.apply) :+ LiteralValueMeta("c", "/abc")
    val meta = ConcatValueMeta(testName, testValues)
    val columnExp = sf.concat(testValues.map(_.toColumn): _*).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(
      sf.expr("CONCAT('http://www.', CAST(ix AS string), '.com/')").as("a"),
      sf.expr("CAST(ix*ix AS string)").as("b"),
      sf.expr(s"CONCAT('http://www.', CAST(ix AS string), '.com/', CAST(ix*ix AS string), '/abc')").as("exp")
    ).select(sf.expr("*"), columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    assertResult(metadataExp)(metadataObs)
    assert(df.where(
      s"exp != $testName").isEmpty,
      df.show(false)
    )
  }
}
