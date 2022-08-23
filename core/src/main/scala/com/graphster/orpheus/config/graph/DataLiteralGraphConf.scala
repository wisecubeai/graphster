package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.graph.DataLiteralGraphConf.{datalit2row, datalitnode}
import com.graphster.orpheus.config.table.StringValueConf
import com.graphster.orpheus.config.types.{ConfFieldType, MetadataField, MetadataFieldType}
import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf}
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.datatypes.{BaseDatatype, RDFDatatype}
import org.apache.jena.ext.xerces.impl.dv.SchemaDVFactory
import org.apache.jena.ext.xerces.impl.dv.xs.XSSimpleTypeDecl
import org.apache.jena.graph.{Node, NodeFactory, Node_Literal}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession}

import java.net.{URI, URISyntaxException}
import scala.util.{Failure, Success, Try}

case class DataLiteralGraphConf(
  lex: ValueConf with AtomicValue,
  datatype: ValueConf with AtomicValue = StringValueConf(DataLiteralGraphConf.DefaultDatatype),
  kwargs: Configuration = Configuration.empty
) extends NodeConf(kwargs.add(
    NodeConf.LexKey -> MetadataField(lex),
    NodeConf.DatatypeKey -> MetadataField(datatype),
  )) {
  override protected val defaultName: String = lex.name

  override val keys: Set[String] = kwargs.keys ++ Set(NodeConf.LexKey, NodeConf.DatatypeKey)

  override val keyTypes: Map[String, MetadataFieldType] = kwargs.keyTypes ++ Map(
    NodeConf.LexKey -> ConfFieldType,
    NodeConf.DatatypeKey -> ConfFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case NodeConf.LexKey => MetadataField(lex)
    case NodeConf.DatatypeKey => MetadataField(datatype)
    case _ => kwargs.get(key)
  }

  override def toColumn: Column = datalit2row(datalitnode(lex.toColumn, datatype.toColumn)).as(name, metadata)
}

object DataLiteralGraphConf extends NodeConfBuilder {
  private val xsdNamespace: String = "http://www.w3.org/2001/XMLSchema"
  val xsdTypes: Map[String, XSDDatatype] = SchemaDVFactory.getInstance().getBuiltInTypes.getEntries
    .filterNot(_.isInstanceOf[String])
    .map(_.asInstanceOf[XSSimpleTypeDecl])
    .map {
      typedecl =>
        (typedecl.getName, new XSDDatatype(typedecl.getName))
    }.toMap
  private val DefaultDatatype: String = "http://www.w3.org/2001/XMLSchema#string"

  override val NodeType: String = "Node_Data_Literal"

  private def getDatatype(datatype: String): RDFDatatype = {
    Try(new URI(datatype)) match {
      case Success(uri) if uri.toString.startsWith(xsdNamespace) => xsdTypes(uri.getFragment)
      case Success(_) if xsdTypes.contains(datatype) => xsdTypes(datatype)
      case Success(_) => new BaseDatatype(datatype)
      case Failure(_: URISyntaxException) if xsdTypes.contains(datatype) => xsdTypes(datatype)
      case _ => throw new IllegalArgumentException(s"Unrecognized datatype - $datatype")
    }
  }

  def parts2jena(lex: String, datatype: String): Node = {
    NodeFactory.createLiteral(lex, getDatatype(datatype)).asInstanceOf[Node_Literal]
  }

  override def jena2row(elem: Node): Row = buildRow(
    lex = elem.getLiteralLexicalForm,
    datatype = elem.getLiteralDatatypeURI
  )

  override def row2jena(row: Row): Node =
    (Option(row.getAs[String](NodeConf.LexKey)), Option(row.getAs[String](NodeConf.DatatypeKey))) match {
      case (Some(lex), Some(dt)) => parts2jena(lex, dt)
      case _ => throw new IllegalArgumentException(s"Incorrect schema (${row.toSeq}) - $row")
    }

  override def jena2string(elem: Node): String =
    s"${elem.toString(true).split("\\^\\^")(0)}^^<${elem.getLiteralDatatypeURI}>"

  override def string2jena(str: String): Node = NodeConf.string2jena(str).asInstanceOf[Node_Literal]

  lazy val datalitnode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("datalitnode", (lex: String, datatype: String) => jena2string(parts2jena(lex, datatype)))
  }

  lazy val datalit2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("datalit2row", (string2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  def apply(
    lex: ValueConf with AtomicValue,
    datatype: String
  ): DataLiteralGraphConf =
    new DataLiteralGraphConf(lex, StringValueConf(datatype))

  def apply(
    lex: String,
    datatype: ValueConf with AtomicValue
  ): DataLiteralGraphConf =
    new DataLiteralGraphConf(StringValueConf(lex), datatype)

  def apply(
    lex: String,
    datatype: String
  ): DataLiteralGraphConf =
    new DataLiteralGraphConf(StringValueConf(lex), StringValueConf(datatype))

  override def apply(config: Configuration): NodeConf = {
    val lex = AtomicValue.fromConfiguration(config.getConf(NodeConf.LexKey))
    val datatype = AtomicValue.fromConfiguration(config.getConf(NodeConf.DatatypeKey))
    val kwargs = config.remove(NodeConf.LexKey).remove(NodeConf.DatatypeKey)
    new DataLiteralGraphConf(lex, datatype, kwargs)
  }
}
