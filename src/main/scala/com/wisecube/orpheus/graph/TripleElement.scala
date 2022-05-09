package com.wisecube.orpheus.graph

import org.apache.jena.graph.{Node, Triple}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.{Metadata, StructField, StructType}
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

case object TripleElement extends OrpheusElement[Triple] {
  val subjectKey: String = "subject"
  val predicateKey: String = "predicate"
  val objectKey: String = "object"

  case class Meta(name: String, subject: NodeMeta, predicate: NodeMeta, `object`: NodeMeta) extends ValueMeta {
    override protected val builder: ValueMetaBuilder[_ >: Meta.this.type] = TripleElement

    override def toColumn: Column = sf.struct(
      subject.toColumn.as(subjectKey, subject.toMetadata),
      predicate.toColumn.as(predicateKey, predicate.toMetadata),
      `object`.toColumn.as(objectKey, `object`.toMetadata),
    ).as(name, toMetadata)

    override def toMetadata: Metadata = buildMetadata
      .putString("name", name)
      .putMetadata(TripleElement.subjectKey, subject.toMetadata)
      .putMetadata(TripleElement.predicateKey, predicate.toMetadata)
      .putMetadata(TripleElement.objectKey, `object`.toMetadata)
      .build()
  }

  override val schema: StructType = StructType(Seq(
    StructField(subjectKey, NodeElement.schema, nullable = false),
    StructField(predicateKey, NodeElement.schema, nullable = false),
    StructField(objectKey, NodeElement.schema, nullable = false),
  ))

  def parts2jena(subject: Node, predicate: Node, `object`: Node): Triple = new Triple(subject, predicate, `object`)

  override def jena2row(elem: Triple): Row = new GenericRowWithSchema(Array(
    NodeElement.jena2row(elem.getSubject),
    NodeElement.jena2row(elem.getPredicate),
    NodeElement.jena2row(elem.getObject)),
    schema
  )

  override def row2jena(row: Row): Triple = new Triple(
    NodeElement.row2jena(row.getAs[Row](subjectKey)),
    NodeElement.row2jena(row.getAs[Row](predicateKey)),
    NodeElement.row2jena(row.getAs[Row](objectKey)),
  )

  override def jena2string(elem: Triple): String = {
    val s = NodeElement.jena2string(elem.getSubject)
    val p = NodeElement.jena2string(elem.getPredicate)
    val o = NodeElement.jena2string(elem.getObject)
    s"$s $p $o ."
  }

  override def string2jena(str: String): Triple = NTripleParser.parse(NTripleParser.triple, str) match {
    case NTripleParser.Success(triple, _) => triple
    case failure: NTripleParser.Failure => throw new IllegalArgumentException(failure.msg)
    case error: NTripleParser.Error => throw new IllegalArgumentException(error.msg)
  }

  lazy val row2triple: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("row2triple", (row2jena _).andThen(jena2string))
  }

  override def fromMetadata(metadata: Metadata): Meta = Meta(
    metadata.getString("name"),
    NodeElement.fromMetadata(metadata.getMetadata(TripleElement.subjectKey)),
    NodeElement.fromMetadata(metadata.getMetadata(TripleElement.predicateKey)),
    NodeElement.fromMetadata(metadata.getMetadata(TripleElement.objectKey)),
  )
}