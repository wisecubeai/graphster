package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.jena.graph.{NodeFactory, Node_Literal}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.MetadataBuilder
import org.apache.spark.sql.{functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class LangLiteralElementSpec extends AnyFunSuite {

  test("Testing conversion between Jena objects, Spark Rows, and strings") {
    val testLex = "abc"
    val testLang = "en"
    val stringExp = s"""\"$testLex\"@$testLang"""
    val nodeExp = NodeFactory.createLiteral(testLex, testLang)
    val rowExp = new GenericRowWithSchema(
      Array(classOf[Node_Literal].getSimpleName, null, null, null, testLex, testLang, null, null),
      LangLiteralElement.schema
    )
    val nodeObs = LangLiteralElement.parts2jena(testLex, testLang)
    assert(nodeExp.matches(nodeObs))
    val rowObs = LangLiteralElement.jena2row(nodeObs)
    assertResult(rowExp)(rowObs)
    assert(nodeExp.matches(LangLiteralElement.row2jena(rowObs)))
    val stringObs = LangLiteralElement.jena2string(nodeObs)
    assertResult(stringExp)(stringObs)
    assert(nodeExp.matches(LangLiteralElement.string2jena(stringObs)))
  }

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testLex = "abc"
    val testLang = "en"
    val testLexMeta = LiteralValueMeta(s"$testName-lex", testLex)
    val testDTMeta = LiteralValueMeta(s"$testName-ln", testLang)
    val metaExp = LangLiteralElement.Meta(testName, testLexMeta, testDTMeta)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, LangLiteralElement.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putMetadata(LangLiteralElement.lexKey, testLexMeta.toMetadata)
      .putMetadata(LangLiteralElement.languageKey, testDTMeta.toMetadata)
      .build()
    val metaObs = LangLiteralElement.fromMetadata(metadataExp)
    val metaObs2 = LangLiteralElement.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    SparkUtils.spark
    val testName = "name"
    val testLex = "abc"
    val testLang = "en"
    val testLexMeta = LiteralValueMeta(s"${testName}_lex", testLex)
    val testLangMeta = LiteralValueMeta(s"${testName}_ln", testLang)
    val stringCol = LangLiteralElement.langlitnode(testLexMeta.toColumn, testLangMeta.toColumn)
    val meta = LangLiteralElement.Meta(testName, testLexMeta, testLangMeta)
    val columnExp = LangLiteralElement.langlit2row(stringCol).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(
      testLexMeta.toColumn,
      testLangMeta.toColumn
    ).select(sf.expr("*"), columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    assertResult(metadataExp)(metadataObs)
    assert(df.selectExpr(s"${testName}_lex = ${testName}.lex AND ${testName}_ln = ${testName}.language")
      .collect().map(_.getBoolean(0)).forall(identity))
  }
}
