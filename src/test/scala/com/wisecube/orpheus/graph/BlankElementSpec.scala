package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.jena.graph.{NodeFactory, Node_Blank}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.MetadataBuilder
import org.scalatest.funsuite.AnyFunSuite

class BlankElementSpec extends AnyFunSuite {

  test("Testing conversion between Jena objects, Spark Rows, and strings") {
    val testBlankId = "abc"
    val stringExp = s"_:$testBlankId"
    val nodeExp = NodeFactory.createBlankNode(testBlankId)
    val rowExp = new GenericRowWithSchema(
      Array(classOf[Node_Blank].getSimpleName, null, null, null, null, null, null, testBlankId, null),
      BlankElement.schema
    )
    val nodeObs = BlankElement.parts2jena(testBlankId)
    assert(nodeExp.matches(nodeObs))
    val rowObs = BlankElement.jena2row(nodeObs)
    assertResult(rowExp)(rowObs)
    assert(nodeExp.matches(BlankElement.row2jena(rowObs)))
    val stringObs = BlankElement.jena2string(nodeObs)
    assertResult(stringExp)(stringObs)
    val nodeObs2 = BlankElement.string2jena(stringObs)
    assert(nodeExp.matches(nodeObs2), s"Expected ($nodeExp) did not match observed ($nodeObs2")
  }

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testBlankId = "abc"
    val testBlankIdMeta = LiteralValueMeta(s"${testName}_blankid", testBlankId)
    val metaExp = BlankElement.Meta(testName, testBlankIdMeta)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, BlankElement.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putMetadata(BlankElement.blankIdKey, testBlankIdMeta.toMetadata)
      .build()
    val metaObs = BlankElement.fromMetadata(metadataExp)
    val metaObs2 = BlankElement.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    SparkUtils.spark
    val testName = "name"
    val testBlankId = "abc"
    val testBlankIdMeta = LiteralValueMeta(s"$testName-blankid", testBlankId)
    val meta = BlankElement.Meta(testName, testBlankIdMeta)
    val columnExp = BlankElement.blank2row(testBlankIdMeta.toColumn).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)
  }
}