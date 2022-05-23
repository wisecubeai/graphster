package com.wisecube.orpheus.graph

import org.apache.jena.graph.{Node, NodeFactory, Node_Blank}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.Metadata
import org.apache.spark.sql.{Column, Row, SparkSession}

case object BlankElement extends NodeElement[Node_Blank] {
  case class Meta(name: String, blankId: AtomicValueMeta) extends NodeMeta {
    protected override val builder: ValueMetaBuilder[_ >: Meta.this.type] = BlankElement

    override def toColumn: Column = blank2row(blankId.toColumn).as(name, toMetadata)

    override def toMetadata: Metadata = buildMetadata
      .putString("name", name)
      .putMetadata(BlankElement.blankIdKey, blankId.toMetadata)
      .build()
  }

  object Meta {
    def apply(name: String, blankId: String): Meta = new Meta(name, LiteralValueMeta(blankId))
  }

  def parts2jena(blankId: String): Node = NodeFactory.createBlankNode(blankId)

  override def jena2row(elem: Node): Row = buildRow(blankId = elem.getBlankNodeLabel)

  override def row2jena(row: Row): Node = Option(row.getAs[String](blankIdKey)) match {
    case Some(bid) => NodeFactory.createBlankNode(bid)
    case None => throw new IllegalArgumentException(s"Incorrect schema ($blankIdKey is null) - $row")
  }

  override def jena2string(elem: Node): String = s"_:${elem.toString(true)}"

  lazy val blanknode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("blanknode", (parts2jena _).andThen(jena2string))
  }

  lazy val blank2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("blank2row", (parts2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  override def fromMetadata(metadata: Metadata): BlankElement.Meta = Meta(
    metadata.getString("name"),
    ValueMeta.fromMetadata(metadata.getMetadata(blankIdKey)).asInstanceOf[AtomicValueMeta]
  )
}
