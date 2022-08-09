package com.graphster.orpheus.query.config

import com.graphster.orpheus.config.types.{BooleanFieldType, ConfFieldType, MetadataField, StringField, StringFieldType}
import com.graphster.orpheus.config.{Conf, Configuration, types}
import com.gsk.kg.config.{Config => BellmanConfig}
import org.apache.spark.sql.types.{Metadata, MetadataBuilder}

case class QueryConfig(
  prefixes: Prefixes = QueryConfig.PrefixesDefault,
  isDefaultGraphExclusive: Boolean = false,
  stripQuestionMarksOnOutput: Boolean = true,
  formatRdfOutput: Boolean = false,
  typeDataframe: Boolean = true
) extends Conf {
  override def metadata: Metadata = new MetadataBuilder()
    .putMetadata(QueryConfig.PrefixesKey, prefixes.metadata)
    .putBoolean(QueryConfig.IsDefaultGraphExclusiveKey, isDefaultGraphExclusive)
    .putBoolean(QueryConfig.StripQuestionMarksOnOutputKey, stripQuestionMarksOnOutput)
    .putBoolean(QueryConfig.FormatRdfOutputKey, formatRdfOutput)
    .putBoolean(QueryConfig.TypeDataframeKey, typeDataframe)
    .build()

  override def keys: Set[String] = keyTypes.keySet

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    QueryConfig.PrefixesKey -> ConfFieldType,
    QueryConfig.IsDefaultGraphExclusiveKey -> BooleanFieldType,
    QueryConfig.StripQuestionMarksOnOutputKey -> BooleanFieldType,
    QueryConfig.FormatRdfOutputKey -> BooleanFieldType,
    QueryConfig.TypeDataframeKey -> BooleanFieldType,
  )

  override def get(key: String): types.MetadataField[_] = key match {
    case QueryConfig.PrefixesKey => MetadataField(prefixes)
    case QueryConfig.IsDefaultGraphExclusiveKey => MetadataField(isDefaultGraphExclusive)
    case QueryConfig.StripQuestionMarksOnOutputKey => MetadataField(stripQuestionMarksOnOutput)
    case QueryConfig.FormatRdfOutputKey => MetadataField(formatRdfOutput)
    case QueryConfig.TypeDataframeKey => MetadataField(typeDataframe)
  }

  val bellmanConfig: BellmanConfig =
    BellmanConfig.default.copy(isDefaultGraphExclusive, stripQuestionMarksOnOutput, formatRdfOutput, typeDataframe)
}

object QueryConfig {
  val PrefixesKey: String = "prefixes"
  val IsDefaultGraphExclusiveKey: String = "isDefaultGraphExclusive"
  val StripQuestionMarksOnOutputKey: String = "stripQuestionMarksOnOutput"
  val FormatRdfOutputKey: String = "formatRdfOutput"
  val TypeDataframeKey: String = "typeDataframe"
  val PrefixesDefault: Prefixes = Prefixes(Map(
    "rdf" -> "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
    "rdfs" -> "<http://www.w3.org/2000/01/rdf-schema#>",
    "schema" -> "<http://schema.org/>",
  ))

  def apply(config: Configuration): QueryConfig = new QueryConfig(
    Prefixes(config.getConf(PrefixesKey)),
    config.getBoolean(IsDefaultGraphExclusiveKey),
    config.getBoolean(StripQuestionMarksOnOutputKey),
    config.getBoolean(FormatRdfOutputKey),
    config.getBoolean(TypeDataframeKey),
  )
}

case class Prefixes(prefixes: Map[String, String]) extends Conf {
  override val metadata: Metadata =
    prefixes.foldLeft(new MetadataBuilder()) { case (mb, (px, ns)) => mb.putString(px, ns) }.build()

  override val keys: Set[String] = prefixes.keySet

  override val keyTypes: Map[String, types.MetadataFieldType] = prefixes.mapValues(_ => StringFieldType)

  override def get(key: String): types.MetadataField[_] = MetadataField(prefixes(key))

  val toSparql: String = prefixes.map { case (px, ns) => s"PREFIX ${px}: $ns" }.mkString("\n")
}

object Prefixes {
  def apply(config: Configuration): Prefixes = new Prefixes(
    config.fields.mapValues { case StringField(ns) => ns }
  )
}