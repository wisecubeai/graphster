package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.types.{ConfFieldType, MetadataField}
import com.graphster.orpheus.config.{Configuration, types}
import com.graphster.orpheus.data.io.NTripleParser
import org.apache.jena.graph.{Node, Triple}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

import scala.util.{Failure, Success, Try}

case class TripleGraphConf(
  subject: NodeConf,
  predicate: NodeConf,
  `object`: NodeConf,
  kwargs: Configuration = Configuration.empty
) extends GraphConf[Triple](kwargs.add(
    TripleGraphConf.SubjectKey -> MetadataField(subject),
    TripleGraphConf.PredicateKey -> MetadataField(predicate),
    TripleGraphConf.ObjectKey -> MetadataField(`object`),
  )) {
  override protected val defaultName: String = TripleGraphConf.DefaultName

  override def keys: Set[String] = kwargs.keys ++ Set(
    TripleGraphConf.SubjectKey,
    TripleGraphConf.PredicateKey,
    TripleGraphConf.ObjectKey,
  )

  override def keyTypes: Map[String, types.MetadataFieldType] = kwargs.keyTypes ++ Map(
    TripleGraphConf.SubjectKey -> ConfFieldType,
    TripleGraphConf.PredicateKey -> ConfFieldType,
    TripleGraphConf.ObjectKey -> ConfFieldType,
  )

  override def get(key: String): MetadataField[_] = key match {
    case TripleGraphConf.SubjectKey => MetadataField(subject)
    case TripleGraphConf.PredicateKey => MetadataField(predicate)
    case TripleGraphConf.ObjectKey => MetadataField(`object`)
    case _ => kwargs.get(key)
  }

  override def toColumn: Column = sf.struct(
    subject.toColumn.as(TripleGraphConf.SubjectKey, subject.metadata),
    predicate.toColumn.as(TripleGraphConf.PredicateKey, predicate.metadata),
    `object`.toColumn.as(TripleGraphConf.ObjectKey, `object`.metadata),
  ).as(name, metadata)
}

object TripleGraphConf extends GraphConfBuilder[Triple, TripleGraphConf] {
  val DefaultName: String = "triple"
  val SubjectKey: String = "subject"
  val PredicateKey: String = "predicate"
  val ObjectKey: String = "object"

  override def schema: StructType = StructType(Seq(
    StructField(TripleGraphConf.SubjectKey, NodeConf.schema),
    StructField(TripleGraphConf.PredicateKey, NodeConf.schema),
    StructField(TripleGraphConf.ObjectKey, NodeConf.schema),
  ))

  def parts2jena(subject: Node, predicate: Node, `object`: Node): Triple = new Triple(subject, predicate, `object`)

  override def jena2row(elem: Triple): Row = new GenericRowWithSchema(Array(
    NodeConf.jena2row(elem.getSubject),
    NodeConf.jena2row(elem.getPredicate),
    NodeConf.jena2row(elem.getObject)),
    schema
  )

  override def row2jena(row: Row): Triple = new Triple(
    NodeConf.row2jena(row.getAs[Row](SubjectKey)),
    NodeConf.row2jena(row.getAs[Row](PredicateKey)),
    NodeConf.row2jena(row.getAs[Row](ObjectKey)),
  )

  override def jena2string(elem: Triple): String = {
    val s = NodeConf.jena2string(elem.getSubject)
    val p = NodeConf.jena2string(elem.getPredicate)
    val o = NodeConf.jena2string(elem.getObject)
    s"$s $p $o ."
  }

  override def string2jena(str: String): Triple = NTripleParser.parse(NTripleParser.triple, str) match {
    case NTripleParser.Success(triple, _) => triple
    case _: NTripleParser.NoSuccess =>
      Try(NTripleParser.fallbackTripleParser(str)) match {
        case Success(triple) => triple
        case Failure(exception) =>
          throw new IllegalArgumentException(s"Cannot parse (with fallback) - $str; exception - $exception")
      }
  }

  lazy val row2triple: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("row2triple", (row2jena _).andThen(jena2string))
  }

  lazy val triple2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("triple2row", (string2jena _).andThen(jena2row).andThen(TripleRow.apply))
  }

  override def apply(config: Configuration): TripleGraphConf = {
    val subject = NodeConf(config.getConf(SubjectKey))
    val predicate = NodeConf(config.getConf(PredicateKey))
    val `object` = NodeConf(config.getConf(ObjectKey))
    val kwargs = config.remove(SubjectKey).remove(PredicateKey).remove(ObjectKey)
    new TripleGraphConf(subject, predicate, `object`, kwargs)
  }
}

case class TripleRow(
  subject: NodeRow,
  predicate: NodeRow,
  `object`: NodeRow
)

object TripleRow {
  def apply(row: Row): TripleRow = row match {
    case Row(subject: Row, predicate: Row, `object`: Row) =>
      TripleRow(NodeRow(subject), NodeRow(predicate), NodeRow(`object`))
  }
}