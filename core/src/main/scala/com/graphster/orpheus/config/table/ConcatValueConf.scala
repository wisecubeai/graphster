package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf, ValueConfBuilder, types}
import com.graphster.orpheus.config.types.{ConfSeqField, ConfSeqFieldType, MetadataField, StringFieldType}
import org.apache.spark.sql.{Column, functions => sf}

case class ConcatValueConf(values: Seq[ValueConf with AtomicValue], kwargs: Configuration = Configuration.empty)
  extends ValueConf(kwargs.add(ConcatValueConf.ValuesKey -> ConfSeqField(values))) {
  override lazy val defaultName: String =
    values.map(_.name.replace("`", ""))
      .mkString("`concat(", ", ", ")`")

  override def toColumn: Column = sf.concat(values.map(_.toColumn): _*).as(name, metadata)

  override def keys: Set[String] = kwargs.keys ++ Set(ConcatValueConf.ValuesKey)

  override def keyTypes: Map[String, types.MetadataFieldType] =
    kwargs.keyTypes + (ConcatValueConf.ValuesKey -> ConfSeqFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case ConcatValueConf.ValuesKey => ConfSeqField(values)
    case _ => kwargs.get(key)
  }
}

object ConcatValueConf extends ValueConfBuilder[ConcatValueConf] {
  val ValuesKey = "values"

  def apply(
    values: Seq[ValueConf with AtomicValue],
    field: (String, MetadataField[_]),
    fields: (String, MetadataField[_])*
  ): ConcatValueConf = new ConcatValueConf(values, Configuration(fields: _*).add(field))

  override def apply(config: Configuration): ConcatValueConf = {
    val values = config.getConfSeq(ValuesKey).map(AtomicValue.fromConfiguration)
    val kwargs = config.remove(ValuesKey)
    new ConcatValueConf(values, kwargs)
  }
}