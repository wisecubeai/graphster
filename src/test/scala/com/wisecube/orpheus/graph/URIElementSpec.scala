package com.wisecube.orpheus.graph

import com.wisecube.orpheus.utils.SparkUtils
import org.apache.jena.graph.{NodeFactory, Node_URI}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.MetadataBuilder
import org.apache.spark.sql.{functions => sf}
import org.scalatest.funsuite.AnyFunSuite

class  URIElementSpec extends AnyFunSuite {
  test("Testing conversion between Jena objects, Spark Rows, and strings") {
    val testNamespace = "http://www.example.org/"
    val testLocal = "abc"
    val testURI = testNamespace + testLocal
    val stringExp = s"<$testURI>"
    val nodeExp = NodeFactory.createURI(testURI)
    val rowExp = new GenericRowWithSchema(
      Array(classOf[Node_URI].getSimpleName, testURI, testNamespace, testLocal, null, null, null, null, null),
      URIElement.schema
    )
    val nodeObs = URIElement.parts2jena(testURI)
    assert(nodeExp.matches(nodeObs))
    val rowObs = URIElement.jena2row(nodeObs)
    assertResult(rowExp)(rowObs)
    assert(nodeExp.matches(URIElement.row2jena(rowObs)))
    val stringObs = URIElement.jena2string(nodeObs)
    assertResult(stringExp)(stringObs)
    assertResult(nodeExp)(URIElement.string2jena(stringObs))
  }

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val testName = "name"
    val testURI: String = "http://www.example.org/abc"
    val testURIMeta = LiteralValueMeta(testName, testURI)
    val metaExp = URIElement.Meta(testName, testURIMeta)
    val metadataObs = metaExp.toMetadata
    val metadataExp = new MetadataBuilder()
      .putString(ValueMeta.typeKey, URIElement.getClass.getSimpleName)
      .putString(ValueMeta.nameKey, testName)
      .putMetadata(URIElement.uriKey, testURIMeta.toMetadata)
      .build()
    val metaObs = URIElement.fromMetadata(metadataExp)
    val metaObs2 = URIElement.fromMetadata(metadataObs)
    assertResult(metaExp)(metaObs)
    assertResult(metaExp)(metaObs2)
    assertResult(metadataExp)(metadataObs)
  }

  test("Testing creation of columns") {
    SparkUtils.spark
    val testName = "name"
    val testURI: String = "http://www.example.org/abc"
    val testURIMeta = LiteralValueMeta(s"${testName}_uri", testURI)
    val meta = URIElement.Meta(testName, testURIMeta)
    val columnExp = URIElement.uri2row(testURIMeta.toColumn).as(testName)
    val columnObs = meta.toColumn
    assertResult(columnExp.expr.sql)(columnObs.expr.sql)

    val df = SparkUtils.generateDataFrame(10)(testURIMeta.toColumn).select(sf.expr("*"), columnObs)
    val metadataExp = meta.toMetadata
    val metadataObs = df.schema.find(_.name == testName).get.metadata
    assertResult(metadataExp)(metadataObs)
    assert(df.selectExpr(s"${testName}.uri = ${testName}_uri").collect().map(_.getBoolean(0)).forall(identity))
  }
}