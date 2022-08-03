package com.wisecube.orpheus.config.table

import com.wisecube.orpheus.config._
import com.wisecube.orpheus.config.types.{DoubleFieldType, MetadataField, StringFieldType}
import org.apache.spark.sql.{Column, functions => sf}

trait LiteralValueConf {
  self: ValueConf with AtomicValue =>
  val datatype: String

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
    config.getString(DatatypeKey) match {
      case LongType => LongValueConf(config)
      case DoubleType => DoubleValueConf(config)
      case BooleanType => BooleanValueConf(config)
      case StringType => StringValueConf(config)
    }
}

case class LongValueConf(name: String, value: Long)
  extends ValueConf(Configuration(
    ValueConf.NameKey -> MetadataField(name),
    LiteralValueConf.DatatypeKey -> MetadataField(LiteralValueConf.LongType),
    LiteralValueConf.ValueKey -> MetadataField(value),
  )) with AtomicValue with LiteralValueConf {
  override val datatype: String = LiteralValueConf.LongType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    LiteralValueConf.DatatypeKey -> StringFieldType,
    LiteralValueConf.ValueKey -> DoubleFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case LiteralValueConf.DatatypeKey => MetadataField(datatype)
    case LiteralValueConf.ValueKey => MetadataField(value)
    case _ => throw new NoSuchElementException()
  }
}

object LongValueConf extends ValueConfBuilder[LongValueConf] {
  def apply(value: Long): LongValueConf = apply(LiteralValueConf.ValueKey, value)

  override def apply(config: Configuration): LongValueConf =
    LongValueConf(config.getString(ValueConf.NameKey), config.getLong(LiteralValueConf.ValueKey))
}

case class DoubleValueConf(name: String, value: Double)
  extends ValueConf(Configuration(
    ValueConf.NameKey -> MetadataField(name),
    LiteralValueConf.DatatypeKey -> MetadataField(LiteralValueConf.DoubleType),
    LiteralValueConf.ValueKey -> MetadataField(value),
  )) with AtomicValue with LiteralValueConf {
  override val datatype: String = LiteralValueConf.DoubleType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    LiteralValueConf.DatatypeKey -> StringFieldType,
    LiteralValueConf.ValueKey -> DoubleFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case LiteralValueConf.DatatypeKey => MetadataField(datatype)
    case LiteralValueConf.ValueKey => MetadataField(value)
  }
}

object DoubleValueConf extends ValueConfBuilder[DoubleValueConf] {
  def apply(value: Double): DoubleValueConf = apply(LiteralValueConf.ValueKey, value)

  override def apply(config: Configuration): DoubleValueConf =
    DoubleValueConf(config.getString(ValueConf.NameKey), config.getDouble(LiteralValueConf.ValueKey))
}

case class BooleanValueConf(name: String, value: Boolean)
  extends ValueConf(Configuration(
    ValueConf.NameKey -> MetadataField(name),
    LiteralValueConf.DatatypeKey -> MetadataField(LiteralValueConf.BooleanType),
    LiteralValueConf.ValueKey -> MetadataField(value),
  )) with AtomicValue with LiteralValueConf {
  override val datatype: String = LiteralValueConf.BooleanType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    LiteralValueConf.DatatypeKey -> StringFieldType,
    LiteralValueConf.ValueKey -> DoubleFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case LiteralValueConf.DatatypeKey => MetadataField(datatype)
    case LiteralValueConf.ValueKey => MetadataField(value)
  }
}

object BooleanValueConf extends ValueConfBuilder[BooleanValueConf] {
  def apply(value: Boolean): BooleanValueConf = apply(LiteralValueConf.ValueKey, value)

  override def apply(config: Configuration): BooleanValueConf =
    BooleanValueConf(config.getString(ValueConf.NameKey), config.getBoolean(LiteralValueConf.ValueKey))
}

case class StringValueConf(name: String, value: String)
  extends ValueConf(Configuration(
    ValueConf.NameKey -> MetadataField(name),
    LiteralValueConf.DatatypeKey -> MetadataField(LiteralValueConf.StringType),
    LiteralValueConf.ValueKey -> MetadataField(value),
  )) with AtomicValue with LiteralValueConf {
  if (name.isEmpty && value.isEmpty) {
    throw new IllegalArgumentException("either name or value must be non-empty/non-blank")
  }

  override val datatype: String = LiteralValueConf.StringType

  override def toColumn: Column = sf.lit(value).as(name, metadata)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    LiteralValueConf.DatatypeKey -> StringFieldType,
    LiteralValueConf.ValueKey -> DoubleFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case LiteralValueConf.DatatypeKey => MetadataField(datatype)
    case LiteralValueConf.ValueKey => MetadataField(value)
  }
}

object StringValueConf extends ValueConfBuilder[StringValueConf] {
  def apply(value: String): StringValueConf = apply(LiteralValueConf.ValueKey, value)

  override def apply(config: Configuration): StringValueConf =
    StringValueConf(config.getString(ValueConf.NameKey), config.getString(LiteralValueConf.ValueKey))
}