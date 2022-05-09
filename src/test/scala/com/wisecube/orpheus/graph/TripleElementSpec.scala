package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.jena.graph.Triple
import org.apache.jena.graph.NodeFactory
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.MetadataBuilder
import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.{functions => sf}

class TripleElementSpec extends AnyFunSuite {

  test("Testing conversion between Jena objects, Spark Rows, and strings") {
    val subjectNode = NodeFactory.createBlankNode("xyz")
    val subjectRow = BlankElement.jena2row(subjectNode)
    val subjectString = BlankElement.jena2string(subjectNode)
    val predicateNode = NodeFactory.createURI("http://www.example.org/abc")
    val predicateRow = URIElement.jena2row(predicateNode)
    val predicateString = URIElement.jena2string(predicateNode)
    val objectNode = NodeFactory.createLiteral("abc", "en")
    val objectRow = LangLiteralElement.jena2row(objectNode)
    val objectString = LangLiteralElement.jena2string(objectNode)
    val tripleExp = new Triple(subjectNode, predicateNode, objectNode)
    val rowExp = new GenericRowWithSchema(Array(subjectRow, predicateRow, objectRow), TripleElement.schema)
    val stringExp = s"$subjectString $predicateString $objectString ."
    val tripleObs = TripleElement.parts2jena(subjectNode, predicateNode, objectNode)
    assert(tripleExp.matches(tripleObs), s"Expected ($tripleExp) did not match observed ($tripleObs")
    val rowObs = TripleElement.jena2row(tripleObs)
    assertResult(rowExp)(rowObs)
    val tripleObs2 = TripleElement.row2jena(rowObs)
    assert(tripleExp.matches(tripleObs2), s"Expected ($tripleExp) did not match observed ($tripleObs2")
    val stringObs = TripleElement.jena2string(tripleObs)
    assertResult(stringExp)(stringObs)
    val tripleObs3 = TripleElement.string2jena(stringObs)
    assert(tripleExp.matches(tripleObs3), s"Expected ($tripleExp) did not match observed ($tripleObs3")
  }

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testBlankId = "abc"
    val testBlankIdMeta = LiteralValueMeta(s"${testName}_blankid", testBlankId)
    val subjectExp = BlankElement.Meta(s"${testName}_s", testBlankIdMeta)
    val testURI: String = "http://www.example.org/abc"
    val testURIMeta = LiteralValueMeta(testName, testURI)
    val predicateExp = URIElement.Meta(s"${testName}_p", testURIMeta)
    val testLex = "abc"
    val testLang = "en"
    val testLexMeta = LiteralValueMeta(s"$testName-lex", testLex)
    val testDTMeta = LiteralValueMeta(s"$testName-ln", testLang)
    val objectExp = LangLiteralElement.Meta(s"${testName}_o", testLexMeta, testDTMeta)
    val metaExp = TripleElement.Meta(testName, subjectExp, predicateExp, objectExp)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, TripleElement.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putMetadata(TripleElement.subjectKey, subjectExp.toMetadata)
      .putMetadata(TripleElement.predicateKey, predicateExp.toMetadata)
      .putMetadata(TripleElement.objectKey, objectExp.toMetadata)
      .build()
    val metaObs = TripleElement.fromMetadata(metadataExp)
    val metaObs2 = TripleElement.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    SparkUtils.spark
    val testName = "name"
    val testBlankId = "abc"
    val testBlankIdMeta = LiteralValueMeta(s"${testName}_blankid", testBlankId)
    val subjectExp = BlankElement.Meta("s", testBlankIdMeta)
    val testURI: String = "http://www.example.org/abc"
    val testURIMeta = LiteralValueMeta(testName, testURI)
    val predicateExp = URIElement.Meta("p", testURIMeta)
    val testLex = "abc"
    val testLang = "en"
    val testLexMeta = LiteralValueMeta(s"${testName}_lex", testLex)
    val testDTMeta = LiteralValueMeta(s"${testName}_ln", testLang)
    val objectExp = LangLiteralElement.Meta("o", testLexMeta, testDTMeta)
    val meta = TripleElement.Meta(testName, subjectExp, predicateExp, objectExp)
    val columnExp = sf.struct(
      subjectExp.toColumn.as(TripleElement.subjectKey, subjectExp.toMetadata),
      predicateExp.toColumn.as(TripleElement.predicateKey, predicateExp.toMetadata),
      objectExp.toColumn.as(TripleElement.objectKey, objectExp.toMetadata)
    ).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(
      subjectExp.toColumn,
      predicateExp.toColumn,
      objectExp.toColumn,
    ).select(sf.expr("*"), columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    NodeElement.row2string
    assertResult(metadataExp)(metadataObs)
    assert(df.selectExpr(Seq(
      s"row2string(${testName}.${TripleElement.subjectKey}) = row2string(s)",
      s"row2string(${testName}.${TripleElement.predicateKey}) = row2string(p)",
      s"row2string(${testName}.${TripleElement.objectKey}) = row2string(o)",
    ).mkString(" AND "))
      .collect().map(_.getBoolean(0)).forall(identity))
  }
}
