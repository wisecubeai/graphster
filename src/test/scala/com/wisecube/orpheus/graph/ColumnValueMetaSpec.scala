package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.sql.types.MetadataBuilder
import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.{functions => sf}

class ColumnValueMetaSpec extends AnyFunSuite {

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testVal: String = "col"
    val metaExp = ColumnValueMeta(testName, testVal)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, ColumnValueMeta.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putString(ColumnValueMeta.columnKey, testVal).build()
    val metaObs = ColumnValueMeta.fromMetadata(metadataExp)
    val metaObs2 = ColumnValueMeta.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    val testName = "name"
    val testVal: String = "col"
    val meta = ColumnValueMeta(testName, testVal)
    val columnExp = sf.col(testVal).cast("string").as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(sf.lit("abc").as(testVal)).select(sf.expr("*"), columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    assertResult(metadataExp)(metadataObs)
    assert(df.selectExpr(s"$testName = $testVal").collect().map(_.getBoolean(0)).forall(identity))
  }
}
