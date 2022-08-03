package com.wisecube.orpheus.config.graph

import com.wisecube.orpheus.Utils._
import com.wisecube.orpheus.config.Configuration
import com.wisecube.orpheus.data.io.NTripleParser
import org.apache.jena.graph._
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}

abstract class NodeConf(config: Configuration) extends GraphConf[Node](config: Configuration)

trait NodeConfBuilder extends GraphConfBuilder[Node, NodeConf] {
  val NodeType: String

  override def schema: StructType = StructType(Seq(
    StructField(NodeConf.TypeKey, StringType, nullable = false),
    StructField(NodeConf.UriKey, StringType, nullable = true),
    StructField(NodeConf.NamespaceKey, StringType, nullable = true),
    StructField(NodeConf.LocalKey, StringType, nullable = true),
    StructField(NodeConf.LexKey, StringType, nullable = true),
    StructField(NodeConf.LanguageKey, StringType, nullable = true),
    StructField(NodeConf.DatatypeKey, StringType, nullable = true),
    StructField(NodeConf.BlankIdKey, StringType, nullable = true),
    StructField(NodeConf.ErrorKey, StringType, nullable = true),
  ))

  protected def buildRow(
    `type`: String = NodeType,
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
}

object NodeConf extends NodeConfBuilder {
  private val LangString: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"

  override val NodeType: String = "Node"

  val TypeKey: String = "type"
  val UriKey: String = "uri"
  val NamespaceKey: String = "namespace"
  val LocalKey: String = "local"
  val LexKey: String = "lex"
  val LanguageKey: String = "language"
  val DatatypeKey: String = "datatype"
  val BlankIdKey: String = "blankId"
  val ErrorKey: String = "error"

  override def jena2row(elem: Node): Row = elem match {
    case n: Node_URI => URIGraphConf.jena2row(n)
    case n: Node_Literal if n.getLiteralDatatypeURI == LangString => LangLiteralGraphConf.jena2row(n)
    case n: Node_Literal if n.getLiteralDatatypeURI != LangString => DataLiteralGraphConf.jena2row(n)
    case n: Node_Blank => BlankGraphConf.jena2row(n)
    case n: Node_Error => ErrorGraphConf.jena2row(n)
  }

  override def row2jena(row: Row): Node = row.getAs[String](TypeKey) match {
    case URIGraphConf.NodeType => URIGraphConf.row2jena(row)
    case LangLiteralGraphConf.NodeType => LangLiteralGraphConf.row2jena(row)
    case DataLiteralGraphConf.NodeType => DataLiteralGraphConf.row2jena(row)
    case BlankGraphConf.NodeType => BlankGraphConf.row2jena(row)
    case ErrorGraphConf.NodeType => ErrorGraphConf.row2jena(row)
  }

  override def jena2string(elem: Node): String = elem match {
    case n: Node_URI => URIGraphConf.jena2string(n)
    case n: Node_Literal if n.getLiteralDatatypeURI == LangString => LangLiteralGraphConf.jena2string(n)
    case n: Node_Literal if n.getLiteralDatatypeURI != LangString => DataLiteralGraphConf.jena2string(n)
    case n: Node_Blank => BlankGraphConf.jena2string(n)
    case n: Node_Error => ErrorGraphConf.jena2string(n)
  }

  override def string2jena(str: String): Node = NTripleParser.parse(NTripleParser.node, str) match {
    case NTripleParser.Success(node, _) => node
    case error: NTripleParser.NoSuccess => ErrorGraphConf.parts2jena(error.msg)
  }

  lazy val string2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    val func = (str: String) => {
      val node = trycall(string2jena _) { case error: Throwable => ErrorGraphConf.parts2jena(error) }(str)
      NodeRow(jena2row(node))
    }
    spark.udf.register("string2row", func)
  }

  lazy val row2string: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("row2string", (row2jena _).andThen(jena2string))
  }

  override def apply(config: Configuration): NodeConf = {
    if (config.hasConf(NodeConf.UriKey)) {
      URIGraphConf(config)
    } else if (config.hasConf(NodeConf.LexKey)) {
      if (config.hasConf(NodeConf.DatatypeKey)) {
        DataLiteralGraphConf(config)
      } else {
        LangLiteralGraphConf(config)
      }
    } else if (config.hasConf(NodeConf.BlankIdKey)) {
      BlankGraphConf(config)
    } else {
      ErrorGraphConf(config)
    }
  }
}