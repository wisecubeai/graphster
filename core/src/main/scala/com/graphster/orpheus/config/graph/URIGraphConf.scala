package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.config.{Configuration, ValueConf, types}
import com.graphster.orpheus.config.types.{ConfFieldType, MetadataField}
import org.apache.jena.graph.{Node, NodeFactory, Node_URI}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession}

case class URIGraphConf(name: String, uri: ValueConf)
  extends NodeConf(Configuration(
    ValueConf.NameKey -> MetadataField(name),
    NodeConf.UriKey -> MetadataField(uri),
  )) {

  override val keys: Set[String] = Set(ValueConf.NameKey, NodeConf.UriKey)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(NodeConf.UriKey -> ConfFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case NodeConf.UriKey => MetadataField(uri)
    case _ => throw new NoSuchElementException()
  }

  override def toColumn: Column = URIGraphConf.uri2row(uri.toColumn).as(name, metadata)
}

object URIGraphConf extends NodeConfBuilder {
  override val NodeType: String = "Node_URI"

  def parts2jena(uri: String): Node_URI = NodeFactory.createURI(uri).asInstanceOf[Node_URI]

  override def jena2row(elem: Node): Row = buildRow(
    uri = elem.getURI,
    namespace = elem.getNameSpace,
    local = elem.getLocalName
  )

  override def row2jena(row: Row): Node = Option(row.getAs[String](NodeConf.UriKey)) match {
    case Some(uri) => NodeFactory.createURI(uri).asInstanceOf[Node_URI]
    case None => throw new IllegalArgumentException(s"Incorrect schema (${NodeConf.UriKey} is null) - $row")
  }

  override def jena2string(elem: Node): String = s"<${elem.getURI}>"

  override def string2jena(str: String): Node = NodeConf.string2jena(str).asInstanceOf[Node_URI]

  lazy val urinode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("urinode", (parts2jena _).andThen(jena2string))
  }

  lazy val uri2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("uri2row", (parts2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  def apply(name: String, uri: String): URIGraphConf = new URIGraphConf(name, StringValueConf(uri))

  override def apply(config: Configuration): NodeConf = new URIGraphConf(
    config.getString(ValueConf.NameKey),
    ValueConf.fromConfiguration(config.getConf(NodeConf.UriKey))
  )
}
