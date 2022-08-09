package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf, ValueConfBuilder, types}
import com.graphster.orpheus.config.types.{MetadataField, StringFieldType}
import org.apache.spark.sql.{Column, functions => sf}

case class ColumnValueConf(name: String, expression: String)
  extends ValueConf(
    ValueConf.NameKey -> MetadataField(name),
    ColumnValueConf.ColumnKey -> MetadataField(expression)
  ) with AtomicValue {

  override def toColumn: Column = sf.expr(expression).cast("string").as(name, metadata)

  override def keys: Set[String] = Set(ValueConf.NameKey, ColumnValueConf.ColumnKey)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    ColumnValueConf.ColumnKey -> StringFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case ColumnValueConf.ColumnKey => MetadataField(expression)
    case _ => throw new NoSuchElementException()
  }
}

object ColumnValueConf extends ValueConfBuilder[ColumnValueConf] {
  val ColumnKey = "column"

  override def apply(config: Configuration): ColumnValueConf =
    ColumnValueConf(config.getString(ValueConf.NameKey), config.getString(ColumnValueConf.ColumnKey))
}