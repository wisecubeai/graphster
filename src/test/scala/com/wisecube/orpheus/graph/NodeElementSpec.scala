package com.wisecube.orpheus.graph

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory
import org.scalatest.funsuite.AnyFunSuite

class NodeElementSpec extends AnyFunSuite {
  test("Testing conversion between Jena objects, Spark Rows, and strings") {
    val testNamespace = "http://www.example.org/"
    val testLocal = "abc"
    val testURI = testNamespace + testLocal
    val testLex = "123"
    val testLang = "en"
    val testDT = new XSDDatatype("integer")
    val testBlankId = "xyz"
    val testNodes = Seq(
      (URIElement, NodeFactory.createURI(testURI)),
      (DataLiteralElement, NodeFactory.createLiteral(testLex, testDT)),
      (LangLiteralElement, NodeFactory.createLiteral(testLex, testLang)),
      (BlankElement, NodeFactory.createBlankNode(testBlankId)),
    )

    testNodes.foreach {
      case (specialist, nodeExp) =>
        val rowExp = specialist.jena2row(nodeExp)
        val stringExp = specialist.jena2string(nodeExp)
        val rowObs = NodeElement.jena2row(nodeExp)
        assertResult(rowExp)(rowObs)
        assert(nodeExp.matches(NodeElement.row2jena(rowObs)))
        val stringObs = NodeElement.jena2string(nodeExp)
        val nodeObs2 = NodeElement.string2jena(stringObs)
        assert(nodeExp.matches(nodeObs2), s"Expected ($nodeExp) did not match observed ($nodeObs2")
    }
  }

  test("Testing creation from Metadata, and rebuilding from metadata") {
    val uriMeta = LiteralValueMeta("uri", "http://www.example.org/abc")
    val lexMeta = LiteralValueMeta("lex", "123")
    val langMeta = LiteralValueMeta("language", "en")
    val dtMeta = LiteralValueMeta("datatype", "http://www.w3.org/1999/02/22-rdf-syntax-ns#integer")
    val blankIdMeta = LiteralValueMeta("blankId", "xyz")
    val testMetas = Seq(
      (URIElement, URIElement.Meta("URIElement", uriMeta)),
      (DataLiteralElement, DataLiteralElement.Meta("DataLiteralElement", lexMeta, dtMeta)),
      (LangLiteralElement, LangLiteralElement.Meta("LangLiteralElement", lexMeta, langMeta)),
      (BlankElement, BlankElement.Meta("BlankElement", blankIdMeta)),
    )
    testMetas.foreach {
      case (specialist, metaExp) =>
        val metadataExp = metaExp.toMetadata
        val metaObs = NodeElement.fromMetadata(metadataExp)
        val metadataObs = metaObs.toMetadata
        assertResult(metaExp)(metaObs)
        assertResult(metadataExp)(metadataObs)
    }
  }
}