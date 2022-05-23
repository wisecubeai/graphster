package com.wisecube.orpheus.graph

import com.wisecube.orpheus.Utils._
import org.apache.jena.graph._
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.{Metadata, StringType, StructField, StructType}
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

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
  val errorKey: String = "error"

  override val schema: StructType = StructType(Seq(
    StructField(typeKey, StringType, nullable = false),
    StructField(uriKey, StringType, nullable = true),
    StructField(namespaceKey, StringType, nullable = true),
    StructField(localKey, StringType, nullable = true),
    StructField(lexKey, StringType, nullable = true),
    StructField(languageKey, StringType, nullable = true),
    StructField(datatypeKey, StringType, nullable = true),
    StructField(blankIdKey, StringType, nullable = true),
    StructField(errorKey, StringType, nullable = true),
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
    error: String = null,
  ): Row = new GenericRowWithSchema(
    Array(`type`, uri, namespace, local, lex, language, datatype, blankId, error),
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
    errorCol: Column = sf.lit(null: String),
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
      errorCol,
    )
  }

  override def string2jena(str: String): Node = NTripleParser.parse(NTripleParser.node, str) match {
    case NTripleParser.Success(node, _) => node
    case error: NTripleParser.NoSuccess => ErrorElement.parts2jena(error.msg)
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
  error: String = null,
)

object NodeRow {
  def apply(row: Row): NodeRow = row match {
    case Row(`type`: String, uri: String, namespace: String, local: String, null, null, null, null, null) => new NodeRow(
      `type` = `type`, uri = uri, namespace = namespace, local = local
    )
    case Row(`type`: String, null, null, null, lex: String, language: String, null, null, null) => new NodeRow(
      `type` = `type`, lex = lex, language = language
    )
    case Row(`type`: String, null, null, null, lex: String, null, datatype: String, null, null) => new NodeRow(
      `type` = `type`, lex = lex, datatype = datatype
    )
    case Row(`type`: String, null, null, null, null, null, null, blankId: String, null) => new NodeRow(
      `type` = `type`, blankId = blankId
    )
    case Row(`type`: String, null, null, null, null, null, null, null, error: String) => new NodeRow(
      `type` = `type`, error = error
    )
    case _ => throw new IllegalArgumentException(s"wrong row shape - $row")
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
    case n: Node_Error => buildRow(`type` = classOf[Node_Error].getSimpleName, error = n.error.getMessage)
  }

  override def row2jena(row: Row): Node = row.getAs[String](typeKey) match {
    case "Node_URI" => URIElement.row2jena(row)
    case "Node_Literal" if row.getAs[String](languageKey) != null => LangLiteralElement.row2jena(row)
    case "Node_Literal" => DataLiteralElement.row2jena(row)
    case "Node_Blank" => BlankElement.row2jena(row)
    case "Node_Error" => Node_Error(new Error(row.getAs[String](errorKey)))
  }

  override def jena2string(elem: Node): String = elem match {
    case _: Node_URI => URIElement.jena2string(elem)
    case n: Node_Literal if n.getLiteralDatatypeURI == LangString => LangLiteralElement.jena2string(elem)
    case n: Node_Literal if n.getLiteralDatatypeURI != LangString => DataLiteralElement.jena2string(elem)
    case _: Node_Blank => BlankElement.jena2string(elem)
    case _: Node_Error => null
  }

  lazy val string2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    val func = (str: String) => {
      val node = trycall(string2jena _) { case error: Throwable => ErrorElement.parts2jena(error) }(str)
      NodeRow(jena2row(node))
    }
    spark.udf.register("string2row", func)
  }

  lazy val row2string: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("row2string", (row2jena _).andThen(jena2string))
  }

  override def fromMetadata(metadata: Metadata): NodeMeta = ValueMeta.fromMetadata(metadata).asInstanceOf[NodeMeta]
}

case class Node_Error(error: Throwable) extends Node_Ext[Throwable](error)

case object ErrorElement extends NodeElement[Node_Error] {
  override type Meta = EmptyValueMeta

  def parts2jena(throwable: Throwable): Node_Error = Node_Error(throwable)
  def parts2jena(msg: String): Node_Error = Node_Error(new Error(msg))

  def parts2row(throwable: Throwable): Row = buildRow(throwable.getMessage)
  def parts2row(msg: String): Row = buildRow(msg)

  override def jena2row(elem: Node): Row = buildRow(error = elem.asInstanceOf[Node_Error].error.getMessage)

  override def row2jena(row: Row): Node = Option(row.getAs[String](errorKey)) match {
    case Some(msg) => parts2jena(msg)
    case _ => throw new IllegalArgumentException(s"Incorrect schema (${row.toSeq}) - $row")
  }

  override def jena2string(elem: Node): String = elem.asInstanceOf[Node_Error].error.getMessage

  override def string2jena(str: String): Node = parts2jena(str)

  override def fromMetadata(metadata: Metadata): ErrorElement.Meta = throw new UnsupportedOperationException()
}