package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.{NodeFactory, Node_Literal}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.MetadataBuilder
import org.apache.spark.sql.{functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class DataLiteralElementSpec extends AnyFunSuite {

  test("Testing conversion between Jena objects, Spark Rows, and strings") {
    val testLex = "123"
    val testDT = new XSDDatatype("integer")
    val stringExp = s"""\"$testLex\"^^<${testDT.getURI}>"""
  val nodeExp = NodeFactory.createLiteral(testLex, testDT)
    val rowExp = new GenericRowWithSchema(
      Array(classOf[Node_Literal].getSimpleName, null, null, null, testLex, null, testDT.getURI, null, null),
      DataLiteralElement.schema
    )
    val nodeObs = DataLiteralElement.parts2jena(testLex, testDT.getURI)
    assert(nodeExp.matches(nodeObs), s"Expected $nodeExp did not match observed $nodeObs")
    val rowObs = DataLiteralElement.jena2row(nodeObs)
    assertResult(rowExp)(rowObs)
    assert(nodeExp.matches(DataLiteralElement.row2jena(rowObs)))
    val stringObs = DataLiteralElement.jena2string(nodeObs)
    assertResult(stringExp)(stringObs)
    assert(nodeExp.matches(DataLiteralElement.string2jena(stringObs)))
  }

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testLex = "123"
    val testDT = new XSDDatatype("integer")
    val testLexMeta = LiteralValueMeta(s"$testName-lex", testLex)
    val testDTMeta = LiteralValueMeta(s"$testName-dr", testDT.getURI)
    val metaExp = DataLiteralElement.Meta(testName, testLexMeta, testDTMeta)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, DataLiteralElement.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putMetadata(DataLiteralElement.lexKey, testLexMeta.toMetadata)
      .putMetadata(DataLiteralElement.datatypeKey, testDTMeta.toMetadata)
      .build()
    val metaObs = DataLiteralElement.fromMetadata(metadataExp)
    val metaObs2 = DataLiteralElement.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    SparkUtils.spark
    val testName = "name"
    val testLex = "abc"
    val testDT = new XSDDatatype("string")
    val testLexMeta = LiteralValueMeta(s"${testName}_lex", testLex)
    val testDTMeta = LiteralValueMeta(s"${testName}_dt", testDT.getURI)
    val stringCol = DataLiteralElement.datalitnode(testLexMeta.toColumn, testDTMeta.toColumn)
    val meta = DataLiteralElement.Meta(testName, testLexMeta, testDTMeta)
    val columnExp = DataLiteralElement.datalit2row(stringCol).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(
      testLexMeta.toColumn,
      testDTMeta.toColumn
    ).select(sf.expr("*"), columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    assertResult(metadataExp)(metadataObs)
    assert(df.selectExpr(s"${testName}_lex = ${testName}.lex AND ${testName}_dt = ${testName}.datatype")
      .collect().map(_.getBoolean(0)).forall(identity))
  }
}
