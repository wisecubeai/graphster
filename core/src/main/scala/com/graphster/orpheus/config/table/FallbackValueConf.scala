package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf, ValueConfBuilder}
import com.graphster.orpheus.config._
import com.graphster.orpheus.config.types._
import org.apache.spark.sql.{Column, functions => sf}

case class FallbackValueConf(
  columns: Seq[ValueConf with AtomicValue],
  finalVal: ValueConf,
  kwargs: Configuration = Configuration.empty
) extends ValueConf(kwargs.add(
    FallbackValueConf.ColumnsKey -> ConfSeqField(columns),
    FallbackValueConf.FinalValKey -> MetadataField(finalVal)
  )) {
  override protected lazy val defaultName: String =
    (columns :+ finalVal).map(_.name.replace("`", ""))
      .mkString("`coalesce(", ", ", ")`")

  override def toColumn: Column = sf.coalesce(columns.map(_.toColumn) :+ finalVal.toColumn: _*).as(name, metadata)

  override def keys: Set[String] = kwargs.keys ++ Set(FallbackValueConf.ColumnsKey, FallbackValueConf.FinalValKey)

  override def keyTypes: Map[String, types.MetadataFieldType] = kwargs.keyTypes ++ Map(
    FallbackValueConf.ColumnsKey -> ConfSeqFieldType,
    FallbackValueConf.FinalValKey -> ConfFieldType,
  )

  override def get(key: String): MetadataField[_] = key match {
    case FallbackValueConf.ColumnsKey => ConfSeqField(columns)
    case FallbackValueConf.FinalValKey => MetadataField(finalVal)
    case _ => kwargs.get(key)
  }
}

object FallbackValueConf extends ValueConfBuilder[FallbackValueConf] {
  val ColumnsKey = "columns"
  val FinalValKey = "finalVal"

  def apply(
    columns: Seq[ValueConf with AtomicValue],
    finalVal: String
  ): FallbackValueConf = new FallbackValueConf(columns, StringValueConf(finalVal))

  def apply(
    columns: Seq[ValueConf with AtomicValue],
    finalVal: String,
    field: (String, MetadataField[_]),
    fields: (String, MetadataField[_])*
  ): FallbackValueConf =
    new FallbackValueConf(columns, StringValueConf(finalVal), Configuration(fields: _*).add(field))

  override def apply(config: Configuration): FallbackValueConf = {
    val columns = config.getConfSeq(ColumnsKey).map(AtomicValue.fromConfiguration)
    val finalVal = ValueConf.fromConfiguration(config.getConf(FinalValKey))
    val kwargs = config.remove(ColumnsKey).remove(FinalValKey)
    new FallbackValueConf(columns, finalVal, kwargs)
  }
}