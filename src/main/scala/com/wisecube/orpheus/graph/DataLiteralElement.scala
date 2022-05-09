package com.wisecube.orpheus.graph

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.ext.xerces.impl.dv.SchemaDVFactory
import org.apache.jena.ext.xerces.impl.dv.xs.XSSimpleTypeDecl
import org.apache.jena.graph.{Node, NodeFactory, Node_Literal}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.Metadata
import org.apache.spark.sql.{Column, Row, SparkSession}

import java.net.{URI, URISyntaxException}
import scala.util.{Failure, Success, Try}

case object DataLiteralElement extends NodeElement[Node_Literal] {
  val xsdTypes: Map[String, String] = SchemaDVFactory.getInstance().getBuiltInTypes.getEntries
    .filterNot(_.isInstanceOf[String])
    .map(_.asInstanceOf[XSSimpleTypeDecl])
    .map {
      typedecl =>
        (typedecl.getName, typedecl.getNamespace + "#" + typedecl.getName)
    }.toMap

  case class Meta(name: String, lex: AtomicValueMeta, datatype: AtomicValueMeta) extends NodeMeta {
    override protected val builder: ValueMetaBuilder[_ >: Meta.this.type] = DataLiteralElement

    override def toColumn: Column = datalit2row(datalitnode(lex.toColumn, datatype.toColumn)).as(name, toMetadata)

    override def toMetadata: Metadata = buildMetadata
      .putString("name", name)
      .putMetadata(DataLiteralElement.lexKey, lex.toMetadata)
      .putMetadata(DataLiteralElement.datatypeKey, datatype.toMetadata)
      .build()
  }

  object Meta {
    def apply(name: String, lex: AtomicValueMeta, datatype: String): Meta = new Meta(name, lex, LiteralValueMeta(datatype))
    def apply(name: String, lex: String, datatype: AtomicValueMeta): Meta = new Meta(name, LiteralValueMeta(lex), datatype)
    def apply(name: String, lex: String, datatype: String): Meta = new Meta(name, LiteralValueMeta(lex), LiteralValueMeta(datatype))
  }

  def parts2jena(lex: String, datatype: String): Node = {
    val dt = Try(new URI(datatype)) match {
      case Success(uri) if Option(uri.getScheme).exists(_.startsWith("http")) => uri.getFragment
      case Success(_) => datatype
      case Failure(_: URISyntaxException) if xsdTypes.contains(datatype) => datatype
      case _ => throw new IllegalArgumentException(s"Unrecognized datatype - $datatype")
    }
    NodeFactory.createLiteral(lex, new XSDDatatype(dt))
  }

  override def jena2row(elem: Node): Row = buildRow(
    lex = elem.getLiteralLexicalForm,
    datatype = elem.getLiteralDatatypeURI
  )

  override def row2jena(row: Row): Node =
    (Option(row.getAs[String](lexKey)), Option(row.getAs[String](datatypeKey))) match {
      case (Some(lex), Some(dt)) => parts2jena(lex, dt)
      case _ => throw new IllegalArgumentException(s"Incorrect schema (${row.toSeq}) - $row")
    }

  override def jena2string(elem: Node): String = elem.toString(true).split("\\^\\^").mkString("", "^^<", ">")

  override def string2jena(str: String): Node = NodeElement.string2jena(str)

  lazy val datalitnode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("datalitnode", (lex: String, datatype: String) => jena2string(parts2jena(lex, datatype)))
  }

  lazy val datalit2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("datalit2row", (string2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  override def fromMetadata(metadata: Metadata): DataLiteralElement.Meta = Meta(
    metadata.getString("name"),
    ValueMeta.fromMetadata(metadata.getMetadata(lexKey)).asInstanceOf[AtomicValueMeta],
    ValueMeta.fromMetadata(metadata.getMetadata(datatypeKey)).asInstanceOf[AtomicValueMeta],
  )
}
