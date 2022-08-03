package com.wisecube.orpheus.config.table

import com.wisecube.orpheus.config._
import com.wisecube.orpheus.config.types._
import org.apache.spark.sql.{Column, functions => sf}

case class FallbackValueConf(name: String, columns: Seq[ValueConf with AtomicValue], finalVal: ValueConf)
  extends ValueConf(
    ValueConf.NameKey -> MetadataField(name),
    FallbackValueConf.ColumnsKey -> ConfSeqField(columns),
    FallbackValueConf.FinalValKey -> MetadataField(finalVal)
  ) {
  override def toColumn: Column = sf.coalesce(columns.map(_.toColumn) :+ finalVal.toColumn: _*).as(name, metadata)

  override def keys: Set[String] = Set(ValueConf.NameKey, FallbackValueConf.ColumnsKey, FallbackValueConf.FinalValKey)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    FallbackValueConf.ColumnsKey -> ConfSeqFieldType,
    FallbackValueConf.FinalValKey -> ConfFieldType,
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case FallbackValueConf.ColumnsKey => ConfSeqField(columns)
    case FallbackValueConf.FinalValKey => MetadataField(finalVal)
    case _ => throw new NoSuchElementException()
  }
}

object FallbackValueConf extends ValueConfBuilder[FallbackValueConf] {
  val ColumnsKey = "columns"
  val FinalValKey = "finalVal"

  def apply(name: String, columns: Seq[ValueConf with AtomicValue]): FallbackValueConf =
    apply(name, columns, EmptyValueConf())

  override def apply(config: Configuration): FallbackValueConf =
    FallbackValueConf(
      config.getString(ValueConf.NameKey),
      config.getConfSeq(ColumnsKey).map(AtomicValue.fromConfiguration),
      ValueConf.fromConfiguration(config.getConf(FinalValKey))
    )
}