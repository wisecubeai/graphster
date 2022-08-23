package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf, ValueConfBuilder, types}
import com.graphster.orpheus.config.types.{MetadataField, StringFieldType}
import org.apache.spark.sql.{Column, functions => sf}

case class ColumnValueConf(expression: String, kwargs: Configuration = Configuration.empty)
  extends ValueConf(kwargs.add(ColumnValueConf.ColumnKey -> MetadataField(expression))
  ) with AtomicValue {

  override protected val defaultName: String = s"`$expression`"

  override def toColumn: Column = sf.expr(expression).cast("string").as(name, metadata)

  override def keys: Set[String] = kwargs.keys + ColumnValueConf.ColumnKey

  override def keyTypes: Map[String, types.MetadataFieldType] =
    kwargs.keyTypes + (ColumnValueConf.ColumnKey -> StringFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case ColumnValueConf.ColumnKey => MetadataField(expression)
    case _ => kwargs.get(key)
  }
}

object ColumnValueConf extends ValueConfBuilder[ColumnValueConf] {
  val ColumnKey = "column"

  def apply(
    expression: String,
    field: (String, MetadataField[_]),
    fields: (String, MetadataField[_])*): ColumnValueConf =
    new ColumnValueConf(expression, Configuration(fields: _*).add(field))

  override def apply(config: Configuration): ColumnValueConf = {
    val expression = config.getString(ColumnKey)
    val kwargs = config.remove(ColumnKey)
    new ColumnValueConf(expression, kwargs)
  }
}