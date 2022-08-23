package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.config.types.{ConfFieldType, MetadataField, MetadataFieldType}
import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf}
import org.apache.jena.graph.{Node, NodeFactory, Node_Blank}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession}

case class BlankGraphConf(blankId: ValueConf with AtomicValue, kwargs: Configuration = Configuration.empty)
  extends NodeConf(kwargs.add(NodeConf.BlankIdKey, MetadataField(blankId))) {

  override protected val defaultName: String = blankId.name

  override val keys: Set[String] = kwargs.keys + NodeConf.BlankIdKey

  override val keyTypes: Map[String, MetadataFieldType] = kwargs.keyTypes + (NodeConf.BlankIdKey -> ConfFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case NodeConf.BlankIdKey => MetadataField(blankId)
    case _ => kwargs.get(key)
  }

  override def toColumn: Column = BlankGraphConf.blank2row(blankId.toColumn).as(name, metadata)
}

object BlankGraphConf extends NodeConfBuilder {
  override val NodeType: String = "Node_Blank"

  def parts2jena(blankId: String): Node = NodeFactory.createBlankNode(blankId).asInstanceOf[Node_Blank]

  override def jena2row(elem: Node): Row = buildRow(blankId = elem.getBlankNodeLabel)

  override def row2jena(row: Row): Node = Option(row.getAs[String](NodeConf.BlankIdKey)) match {
    case Some(bid) => NodeFactory.createBlankNode(bid).asInstanceOf[Node_Blank]
    case None => throw new IllegalArgumentException(s"Incorrect schema (${NodeConf.BlankIdKey} is null) - $row")
  }

  override def jena2string(elem: Node): String = s"_:${elem.toString(true)}"

  override def string2jena(str: String): Node = NodeConf.string2jena(str).asInstanceOf[Node_Blank]

  lazy val blanknode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("blanknode", (parts2jena _).andThen(jena2string))
  }

  lazy val blank2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("blank2row", (parts2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  def apply(blankId: String): BlankGraphConf =
    new BlankGraphConf(StringValueConf(blankId))

  override def apply(config: Configuration): BlankGraphConf = {
    val blankId = AtomicValue.fromConfiguration(config.getConf(NodeConf.BlankIdKey))
    val kwargs = config.remove(NodeConf.BlankIdKey)
    new BlankGraphConf(blankId, kwargs)
  }
}
