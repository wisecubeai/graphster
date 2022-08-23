package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.config.types.{ConfFieldType, MetadataField}
import com.graphster.orpheus.config.{Configuration, ValueConf, types}
import org.apache.jena.graph.{Node, NodeFactory, Node_URI}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession}

case class URIGraphConf(uri: ValueConf, kwargs: Configuration = Configuration.empty)
  extends NodeConf(kwargs.add(NodeConf.UriKey -> MetadataField(uri))) {
  override protected val defaultName: String = uri.name

  override val keys: Set[String] = kwargs.keys + NodeConf.UriKey

  override def keyTypes: Map[String, types.MetadataFieldType] = kwargs.keyTypes + (NodeConf.UriKey -> ConfFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case NodeConf.UriKey => MetadataField(uri)
    case _ => kwargs.get(key)
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

  def apply(uri: ValueConf, field: (String, MetadataField[_]), fields: (String, MetadataField[_])*): URIGraphConf =
    apply(uri, Configuration(fields: _*).add(field))

  def apply(uri: String): URIGraphConf =
    new URIGraphConf(StringValueConf(uri))

  def apply(uri: String, field: (String, MetadataField[_]), fields: (String, MetadataField[_])*): URIGraphConf =
    apply(StringValueConf(uri), Configuration(fields: _*).add(field))

  override def apply(config: Configuration): NodeConf = {
    val uri = ValueConf.fromConfiguration(config.getConf(NodeConf.UriKey))
    val kwargs = config.remove(NodeConf.UriKey)
    new URIGraphConf(uri, kwargs)
  }
}
