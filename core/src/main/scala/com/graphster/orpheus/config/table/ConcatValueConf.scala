package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf, ValueConfBuilder, types}
import com.graphster.orpheus.config.types.{ConfSeqField, ConfSeqFieldType, MetadataField, StringFieldType}
import org.apache.spark.sql.{Column, functions => sf}

case class ConcatValueConf(name: String, values: Seq[ValueConf with AtomicValue])
  extends ValueConf(
    ValueConf.NameKey -> MetadataField(name),
    ConcatValueConf.ValuesKey -> ConfSeqField(values)
  ) {
  override def toColumn: Column = sf.concat(values.map(_.toColumn): _*).as(name, metadata)

  override def keys: Set[String] = Set(ValueConf.NameKey, ConcatValueConf.ValuesKey)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey-> StringFieldType,
    ConcatValueConf.ValuesKey -> ConfSeqFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case ConcatValueConf.ValuesKey => ConfSeqField(values)
    case _ => throw new NoSuchElementException()
  }
}

object ConcatValueConf extends ValueConfBuilder[ConcatValueConf] {
  val ValuesKey = "values"

  override def apply(config: Configuration): ConcatValueConf =
    ConcatValueConf(
      config.getString(ValueConf.NameKey),
      config.getConfSeq(ValuesKey).map(AtomicValue.fromConfiguration)
    )
}