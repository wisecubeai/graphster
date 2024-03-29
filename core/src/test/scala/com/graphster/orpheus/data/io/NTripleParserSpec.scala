package com.graphster.orpheus.data.io

import com.graphster.orpheus.config.graph.TripleGraphConf
import com.graphster.orpheus.config.graph.TripleGraphConf
import org.apache.jena.graph.{NodeFactory, Triple}
import org.scalatest.funsuite.AnyFunSuite

class NTripleParserSpec extends AnyFunSuite {
  test("test possible triple arrangements") {
    val uriSubject = NodeFactory.createURI("http://www.example.org/subject")
    val uriPredicate = NodeFactory.createURI("http://www.example.org/predicate")
    val uriObject = NodeFactory.createURI("http://www.example.org/object")
    val literalObject = NodeFactory.createLiteral("literal", "en")
    val literalMWObject = NodeFactory.createLiteral("trouble \\\" literal", "en")
    val blankSubject = NodeFactory.createBlankNode("xyz")
    val blankObject = NodeFactory.createBlankNode("abc")

    val uri2uriExp = new Triple(uriSubject, uriPredicate, uriObject)
    val uri2litExp = new Triple(uriSubject, uriPredicate, literalObject)
    val uri2blankExp = new Triple(uriSubject, uriPredicate, blankObject)
    val blank2uriExp = new Triple(blankSubject, uriPredicate, uriObject)
    val blank2litExp = new Triple(blankSubject, uriPredicate, literalMWObject)
    val blank2blankExp = new Triple(blankSubject, uriPredicate, blankObject)

    val uri2uriStr = TripleGraphConf.jena2string(uri2uriExp)
    val uri2litStr = TripleGraphConf.jena2string(uri2litExp)
    val uri2blankStr = TripleGraphConf.jena2string(uri2blankExp)
    val blank2uriStr = TripleGraphConf.jena2string(blank2uriExp)
    val blank2litStr = TripleGraphConf.jena2string(blank2litExp)
    val blank2blankStr = TripleGraphConf.jena2string(blank2blankExp)

    val uri2uriObs = NTripleParser.parse(NTripleParser.triple, uri2uriStr).get
    val uri2litObs = NTripleParser.parse(NTripleParser.triple, uri2litStr).get
    val uri2blankObs = NTripleParser.parse(NTripleParser.triple, uri2blankStr).get
    val blank2uriObs = NTripleParser.parse(NTripleParser.triple, blank2uriStr).get
    val blank2litObs = NTripleParser.parse(NTripleParser.triple, blank2litStr).get
    val blank2blankObs = NTripleParser.parse(NTripleParser.triple, blank2blankStr).get

    assertResult(uri2uriExp)(uri2uriObs)
    assertResult(uri2litExp)(uri2litObs)
    assertResult(uri2blankExp)(uri2blankObs)
    assertResult(blank2uriExp)(blank2uriObs)
    assertResult(blank2litExp)(blank2litObs)
    assertResult(blank2blankExp)(blank2blankObs)
  }

  test("troublesome lines") {
    val lines = Seq(
      "<http://id.nlm.nih.gov/mesh/A01>\t<http://id.nlm.nih.gov/mesh/vocab#active>\t\"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> ."
    )
  }
}
