package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.table.StringValueConf
import LangLiteralGraphConf.{langlit2row, langlitnode}
import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf}
import com.graphster.orpheus.config.types.{ConfFieldType, MetadataField, MetadataFieldType, StringFieldType}
import org.apache.jena.graph.{Node, NodeFactory, Node_Literal}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession}

case class LangLiteralGraphConf(name: String, lex: ValueConf with AtomicValue, language: ValueConf with AtomicValue)
  extends NodeConf(Configuration(
    ValueConf.NameKey -> MetadataField(name),
    NodeConf.LexKey -> MetadataField(lex),
    NodeConf.LanguageKey -> MetadataField(language),
  )) {

  override val keys: Set[String] = Set(ValueConf.NameKey, NodeConf.LexKey, NodeConf.LanguageKey)

  override val keyTypes: Map[String, MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    NodeConf.LexKey -> ConfFieldType,
    NodeConf.LanguageKey -> ConfFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case NodeConf.LexKey => MetadataField(lex)
    case NodeConf.LanguageKey => MetadataField(language)
    case _ => throw new NoSuchElementException()
  }
  override def toColumn: Column = langlit2row(langlitnode(lex.toColumn, language.toColumn)).as(name, metadata)
}

object LangLiteralGraphConf extends NodeConfBuilder {

  override val NodeType: String = "Node_Lang_Literal"

  def parts2jena(lex: String, lang: String): Node_Literal =
    NodeFactory.createLiteral(lex, lang).asInstanceOf[Node_Literal]

  override def jena2row(elem: Node): Row = buildRow(
    lex = elem.getLiteralLexicalForm,
    language = elem.getLiteralLanguage
  )

  override def row2jena(row: Row): Node =
    (Option(row.getAs[String](NodeConf.LexKey)), Option(row.getAs[String](NodeConf.LanguageKey))) match {
      case (Some(lex), Some(lang)) => parts2jena(lex, lang)
      case _ => throw new IllegalArgumentException(s"Incorrect schema (${row.toSeq}) - $row")
    }

  override def jena2string(elem: Node): String = elem.toString(true)

  override def string2jena(str: String): Node = NodeConf.string2jena(str).asInstanceOf[Node_Literal]

  lazy val langlitnode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("langlitnode", (lex: String, lang: String) => jena2string(parts2jena(lex, lang)))
  }

  lazy val langlit2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("langlit2row", (string2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  def apply(name: String, lex: ValueConf with AtomicValue, language: String): LangLiteralGraphConf =
    new LangLiteralGraphConf(name, lex, StringValueConf(language))

  def apply(name: String, lex: String, language: ValueConf with AtomicValue): LangLiteralGraphConf =
    new LangLiteralGraphConf(name, StringValueConf(lex), language)

  def apply(name: String, lex: String, language: String): LangLiteralGraphConf =
    new LangLiteralGraphConf(name, StringValueConf(lex), StringValueConf(language))

  override def apply(config: Configuration): NodeConf = new LangLiteralGraphConf(
    config.getString(ValueConf.NameKey),
    AtomicValue.fromConfiguration(config.getConf(NodeConf.LexKey)),
    AtomicValue.fromConfiguration(config.getConf(NodeConf.LanguageKey))
  )

}
