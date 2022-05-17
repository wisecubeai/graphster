package com.wisecube.orpheus.graph

import org.apache.jena.graph.{Node, NodeFactory, Node_URI}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.Metadata
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

case object URIElement extends NodeElement[Node_URI] {
  case class Meta(name: String, uri: AtomicValueMeta) extends NodeMeta {
    override protected val builder: ValueMetaBuilder[_ >: Meta.this.type] = URIElement

    override def toColumn: Column = uri2row(uri.toColumn).as(name, toMetadata)

    override def toMetadata: Metadata = buildMetadata
      .putMetadata(URIElement.uriKey, uri.toMetadata)
      .build()
  }

  object Meta {
    def apply(name: String, uri: String): Meta = new Meta(name, LiteralValueMeta(uri))
  }

  def parts2jena(uri: String): Node = NodeFactory.createURI(uri)

  override def jena2row(elem: Node): Row = buildRow(
    uri = elem.getURI,
    namespace = elem.getNameSpace,
    local = elem.getLocalName
  )

  override def row2jena(row: Row): Node = Option(row.getAs[String](uriKey)) match {
    case Some(uri) => NodeFactory.createURI(uri)
    case None => throw new IllegalArgumentException(s"Incorrect schema ($uriKey is null) - $row")
  }

  override def jena2string(elem: Node): String = s"<${elem.getURI}>"

  override def string2jena(str: String): Node = NodeElement.string2jena(str)

  lazy val urinode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("urinode", (parts2jena _).andThen(jena2string))
  }

  lazy val uri2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("uri2row", (parts2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  override def fromMetadata(metadata: Metadata): URIElement.Meta = Meta(
    metadata.getString("name"),
    ValueMeta.fromMetadata(metadata.getMetadata(uriKey)).asInstanceOf[AtomicValueMeta]
  )
}
