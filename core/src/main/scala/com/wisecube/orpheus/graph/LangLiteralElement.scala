package com.wisecube.orpheus.graph

import org.apache.jena.graph.{Node, NodeFactory, Node_Literal}
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types.Metadata
import org.apache.spark.sql.{Column, Row, SparkSession}

case object LangLiteralElement extends NodeElement[Node_Literal] {
  case class Meta(name: String, lex: AtomicValueMeta, language: AtomicValueMeta) extends NodeMeta {
    protected override val builder: ValueMetaBuilder[_ >: Meta.this.type] = LangLiteralElement

    override def toColumn: Column = langlit2row(langlitnode(lex.toColumn, language.toColumn)).as(name, toMetadata)

    override def toMetadata: Metadata = buildMetadata
      .putString("name", name)
      .putMetadata(DataLiteralElement.lexKey, lex.toMetadata)
      .putMetadata(DataLiteralElement.languageKey, language.toMetadata)
      .build()
  }

  object Meta {
    def apply(name: String, lex: AtomicValueMeta, language: String): Meta = new Meta(name, lex, LiteralValueMeta(language))
    def apply(name: String, lex: String, language: AtomicValueMeta): Meta = new Meta(name, LiteralValueMeta(lex), language)
    def apply(name: String, lex: String, language: String): Meta = new Meta(name, LiteralValueMeta(lex), LiteralValueMeta(language))
  }

  def parts2jena(lex: String, lang: String): Node =
    NodeFactory.createLiteral(lex, lang)

  override def jena2row(elem: Node): Row = buildRow(
    lex = elem.getLiteralLexicalForm,
    language = elem.getLiteralLanguage
  )

  override def row2jena(row: Row): Node =
    (Option(row.getAs[String](lexKey)), Option(row.getAs[String](languageKey))) match {
      case (Some(lex), Some(lang)) => parts2jena(lex, lang)
      case _ => throw new IllegalArgumentException(s"Incorrect schema (${row.toSeq}) - $row")
    }

  override def jena2string(elem: Node): String = elem.toString(true)

  lazy val langlitnode: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("langlitnode", (lex: String, lang: String) => jena2string(parts2jena(lex, lang)))
  }

  lazy val langlit2row: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("langlit2row", (string2jena _).andThen(jena2row).andThen(NodeRow.apply))
  }

  override def fromMetadata(metadata: Metadata): LangLiteralElement.Meta = Meta(
    metadata.getString("name"),
    ValueMeta.fromMetadata(metadata.getMetadata(lexKey)).asInstanceOf[AtomicValueMeta],
    ValueMeta.fromMetadata(metadata.getMetadata(languageKey)).asInstanceOf[AtomicValueMeta],
  )
}
