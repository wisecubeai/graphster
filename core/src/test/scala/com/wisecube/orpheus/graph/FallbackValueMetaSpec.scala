package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.sql.types.MetadataBuilder
import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.{functions => sf}

class FallbackValueMetaSpec extends AnyFunSuite {

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testColumns = Seq(
      ColumnValueMeta("a"),
      ColumnValueMeta("b"),
      ColumnValueMeta("c"),
    )
    val testFinalVal = LiteralValueMeta("final", "abc")
    val metaExp = FallbackValueMeta(testName, testColumns, testFinalVal)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, FallbackValueMeta.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putMetadataArray(FallbackValueMeta.columnsKey, testColumns.map(_.toMetadata).toArray)
      .putMetadata(FallbackValueMeta.finalValKey, testFinalVal.toMetadata)
      .build()
    val metaObs = FallbackValueMeta.fromMetadata(metadataExp)
    val metaObs2 = FallbackValueMeta.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    val testName = "name"
    val testCols = Seq("a", "b")
    val testColumns = testCols.map(ColumnValueMeta.apply) :+ EmptyValueMeta()
    val testFinalVal = LiteralValueMeta("final", "abc")
    val meta = FallbackValueMeta(testName, testColumns, testFinalVal)
    val columnExp = sf.coalesce(testColumns.map(_.toColumn) :+ testFinalVal.toColumn: _*).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(
      sf.expr("IF(ix % 2 = 0, null, 'a')").as("a"),
      sf.lit("b").as("b")
    ).select(sf.expr("*"), columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    assertResult(metadataExp)(metadataObs)
    assert(df.selectExpr(s"IF(a IS NOT NULL, a, b) = $testName").collect().map(_.getBoolean(0)).forall(identity))
  }

}
