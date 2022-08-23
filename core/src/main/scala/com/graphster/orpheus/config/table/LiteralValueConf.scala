package com.graphster.orpheus.config.table

import com.graphster.orpheus.config._
import com.graphster.orpheus.config.types._
import org.apache.spark.sql.{Column, functions => sf}

trait LiteralValueConf {
  self: ValueConf with AtomicValue =>
  val datatype: String

  override protected def defaultName: String = s"${datatype}_${LiteralValueConf.ValueKey}"

  override def keys: Set[String] = Set(ValueConf.NameKey, LiteralValueConf.DatatypeKey, LiteralValueConf.ValueKey)
}

object LiteralValueConf extends ValueConfBuilder[ValueConf with AtomicValue with LiteralValueConf] {
  val ValueKey = "value"
  val DatatypeKey = "datatype"
  val LongType = "long"
  val DoubleType = "double"
  val BooleanType = "boolean"
  val StringType = "string"

  override def apply(config: Configuration): ValueConf with AtomicValue with LiteralValueConf =
    config.get(ValueKey) match {
      case LongField(_) => LongValueConf(config)
      case DoubleField(_) => DoubleValueConf(config)
      case BooleanField(_) => BooleanValueConf(config)
      case StringField(_) => StringValueConf(config)
    }
}

case class LongValueConf(value: Long, kwargs: Configuration = Configuration.empty)
  extends ValueConf(kwargs.add(LiteralValueConf.ValueKey -> MetadataField(value)))
    with AtomicValue with LiteralValueConf {
  override val datatype: String = LiteralValueConf.LongType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] =
    kwargs.keyTypes + (LiteralValueConf.ValueKey -> DoubleFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case LiteralValueConf.ValueKey => MetadataField(value)
    case _ => kwargs.get(key)
  }
}

object LongValueConf extends ValueConfBuilder[LongValueConf] {
  def apply(value: Long): LongValueConf =
    new LongValueConf(value)

  def apply(value: Long, field: (String, MetadataField[_]), fields: (String, MetadataField[_])*): LongValueConf =
    new LongValueConf(value, Configuration(fields: _*).add(field))

  override def apply(config: Configuration): LongValueConf = {
    val value = config.getLong(LiteralValueConf.ValueKey)
    val kwargs = config.remove(LiteralValueConf.ValueKey)
    new LongValueConf(value, kwargs)
  }
}

case class DoubleValueConf(value: Double, kwargs: Configuration = Configuration.empty)
  extends ValueConf(kwargs.add(LiteralValueConf.ValueKey -> MetadataField(value)))
    with AtomicValue with LiteralValueConf {
  override val datatype: String = LiteralValueConf.DoubleType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] =
    kwargs.keyTypes + (LiteralValueConf.ValueKey -> DoubleFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case LiteralValueConf.ValueKey => MetadataField(value)
    case _ => kwargs.get(key)
  }
}

object DoubleValueConf extends ValueConfBuilder[DoubleValueConf] {
  def apply(value: Double): DoubleValueConf =
    new DoubleValueConf(value)

  def apply(value: Double, field: (String, MetadataField[_]), fields: (String, MetadataField[_])*): DoubleValueConf =
    new DoubleValueConf(value, Configuration(fields: _*).add(field))

  override def apply(config: Configuration): DoubleValueConf = {
    val value = config.getDouble(LiteralValueConf.ValueKey)
    val kwargs = config.remove(LiteralValueConf.ValueKey)
    new DoubleValueConf(value, kwargs)
  }
}

case class BooleanValueConf(value: Boolean, kwargs: Configuration = Configuration.empty)
  extends ValueConf(kwargs.add(LiteralValueConf.ValueKey -> MetadataField(value)))
    with AtomicValue with LiteralValueConf {
  override val datatype: String = LiteralValueConf.BooleanType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] =
    kwargs.keyTypes + (LiteralValueConf.ValueKey -> DoubleFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case LiteralValueConf.ValueKey => MetadataField(value)
    case _ => kwargs.get(key)
  }
}

object BooleanValueConf extends ValueConfBuilder[BooleanValueConf] {
  def apply(value: Boolean): BooleanValueConf =
    new BooleanValueConf(value)

  def apply(value: Boolean, field: (String, MetadataField[_]), fields: (String, MetadataField[_])*): BooleanValueConf =
    new BooleanValueConf(value, Configuration(fields: _*).add(field))

  override def apply(config: Configuration): BooleanValueConf = {
    val value = config.getBoolean(LiteralValueConf.ValueKey)
    val kwargs = config.remove(LiteralValueConf.ValueKey)
    new BooleanValueConf(value, kwargs)
  }
}

case class StringValueConf(value: String, kwargs: Configuration = Configuration.empty)
  extends ValueConf(kwargs.add(LiteralValueConf.ValueKey -> MetadataField(value)))
    with AtomicValue with LiteralValueConf {
  override val datatype: String = LiteralValueConf.StringType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] =
    kwargs.keyTypes + (LiteralValueConf.ValueKey -> DoubleFieldType)

  override def get(key: String): MetadataField[_] = key match {
    case LiteralValueConf.ValueKey => MetadataField(value)
    case _ => kwargs.get(key)
  }
}

object StringValueConf extends ValueConfBuilder[StringValueConf] {
  def apply(value: String): StringValueConf =
    new StringValueConf(value)

  def apply(value: String, field: (String, MetadataField[_]), fields: (String, MetadataField[_])*): StringValueConf =
    new StringValueConf(value, Configuration(fields: _*).add(field))

  override def apply(config: Configuration): StringValueConf = {
    val value = config.getString(LiteralValueConf.ValueKey)
    val kwargs = config.remove(LiteralValueConf.ValueKey)
    new StringValueConf(value, kwargs)
  }
}