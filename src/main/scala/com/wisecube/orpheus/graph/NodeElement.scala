package com.wisecube.orpheus.graph

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.datatypes.xsd.impl.XSDBaseStringType
import org.apache.jena.graph.{Node, Node_Blank, Node_Literal, Node_URI}
import org.apache.jena.sparql.util.NodeFactoryExtra
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.{Metadata, StringType, StructField, StructType}
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

import scala.reflect.ClassTag

abstract class NodeElement[T <: Node: ClassTag] extends OrpheusElement[Node] {
  override type Meta <: NodeMeta

  val typeKey: String = "type"
  val uriKey: String = "uri"
  val namespaceKey: String = "namespace"
  val localKey: String = "local"
  val lexKey: String = "lex"
  val languageKey: String = "language"
  val datatypeKey: String = "datatype"
  val blankIdKey: String = "blankId"

  override val schema: StructType = StructType(Seq(
    StructField(typeKey, StringType, nullable = false),
    StructField(uriKey, StringType, nullable = true),
    StructField(namespaceKey, StringType, nullable = true),
    StructField(localKey, StringType, nullable = true),
    StructField(lexKey, StringType, nullable = true),
    StructField(languageKey, StringType, nullable = true),
    StructField(datatypeKey, StringType, nullable = true),
    StructField(blankIdKey, StringType, nullable = true),
  ))

  protected def buildRow(
    `type`: String = getNodeClass.getSimpleName,
    uri: String = null,
    namespace: String = null,
    local: String = null,
    lex: String = null,
    language: String = null,
    datatype: String = null,
    blankId: String = null,
  ): Row = new GenericRowWithSchema(
    Array(`type`, uri, namespace, local, lex, language, datatype, blankId),
    schema
  )

  protected def buildColumn(
    typeCol: Column = sf.lit(getNodeClass.getSimpleName),
    uriCol: Column = sf.lit(null: String),
    namespaceCol: Column = sf.lit(null: String),
    localCol: Column = sf.lit(null: String),
    lexCol: Column = sf.lit(null: String),
    languageCol: Column = sf.lit(null: String),
    datatypeCol: Column = sf.lit(null: String),
    blankIdCol: Column = sf.lit(null: String),
  ): Column = {
    sf.struct(
      typeCol,
      uriCol,
      namespaceCol,
      localCol,
      lexCol,
      languageCol,
      datatypeCol,
      blankIdCol,
    )
  }

  override def string2jena(str: String): Node = NTripleParser.parse(NTripleParser.node, str) match {
    case NTripleParser.Success(node, _) => node
    case failure: NTripleParser.Failure => throw new IllegalArgumentException(failure.msg)
    case error: NTripleParser.Error => throw new IllegalArgumentException(error.msg)
  }

  protected val getNodeClass: Class[_] = implicitly[ClassTag[T]].runtimeClass
}

case class NodeRow(
  `type`: String,
  uri: String = null,
  namespace: String = null,
  local: String = null,
  lex: String = null,
  language: String = null,
  datatype: String = null,
  blankId: String = null,
)

object NodeRow {
  def apply(row: Row): NodeRow = row match {
    case Row(`type`: String, uri: String, namespace: String, local: String, null, null, null, null) => NodeRow(
      `type` = `type`, uri = uri, namespace = namespace, local = local
    )
    case Row(`type`: String, null, null, null, lex: String, language: String, null, null) => NodeRow(
      `type` = `type`, lex = lex, language = language
    )
    case Row(`type`: String, null, null, null, lex: String, null, datatype: String, null) => NodeRow(
      `type` = `type`, lex = lex, datatype = datatype
    )
    case Row(`type`: String, null, null, null, null, null, null, blankId: String) => NodeRow(
      `type` = `type`, blankId = blankId
    )
    case _ => throw new IllegalArgumentException(s"wrong row shape - ${row}")
  }
}

trait NodeMeta extends ValueMeta

case object NodeElement extends NodeElement[Node] {
  private val LangString: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"
  override type Meta = NodeMeta

  override def jena2row(elem: Node): Row = elem match {
    case _: Node_URI => URIElement.jena2row(elem)
    case n: Node_Literal if n.getLiteralDatatypeURI == LangString => LangLiteralElement.jena2row(elem)
    case n: Node_Literal if n.getLiteralDatatypeURI != LangString => DataLiteralElement.jena2row(elem)
    case _: Node_Blank => BlankElement.jena2row(elem)
  }

  override def row2jena(row: Row): Node = row.getAs[String](typeKey) match {
    case "Node_URI" => URIElement.row2jena(row)
    case "Node_Literal" if row.getAs[String](languageKey) != null => LangLiteralElement.row2jena(row)
    case "Node_Literal" => DataLiteralElement.row2jena(row)
    case "Node_Blank" => BlankElement.row2jena(row)
  }

  override def jena2string(elem: Node): String = elem match {
    case _: Node_URI => URIElement.jena2string(elem)
    case n: Node_Literal if n.getLiteralDatatypeURI == LangString => LangLiteralElement.jena2string(elem)
    case n: Node_Literal if n.getLiteralDatatypeURI != LangString => DataLiteralElement.jena2string(elem)
    case _: Node_Blank => BlankElement.jena2string(elem)
  }

  lazy val string2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("string2row", (string2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  lazy val row2string: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("row2string", (row2jena _).andThen(jena2string))
  }

  override def fromMetadata(metadata: Metadata): NodeMeta = ValueMeta.fromMetadata(metadata).asInstanceOf[NodeMeta]
}