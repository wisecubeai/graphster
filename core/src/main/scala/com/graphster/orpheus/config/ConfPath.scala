package com.graphster.orpheus.config

import com.graphster.orpheus.config
import com.graphster.orpheus.config.types._
import org.apache.spark.sql.types.{Metadata, MetadataBuilder}

case class ConfPath(name: String, value: MetadataField[_]) extends AbstractConfiguration {
  override def keys: Set[String] = value match {
    case ConfField(c) => c.keys
    case _ => Set.empty
  }

  override def keyTypes: Map[String, MetadataFieldType] = value match {
    case ConfField(c) => c.keyTypes
    case _ => Map.empty
  }

  override def get(key: String): MetadataField[_] = value match {
    case ConfField(c) => c.get(key)
    case _ => throw new NotImplementedError("Only Conf fields have members")
  }

  def /(i: Int): ConfPath = getPath(i)

  def get(i: Int): MetadataField[_] = value match {
    case LongSeqField(seq) => MetadataField(seq(i))
    case DoubleSeqField(seq) => MetadataField(seq(i))
    case BooleanSeqField(seq) => MetadataField(seq(i))
    case StringSeqField(seq) => MetadataField(seq(i))
    case ConfSeqField(seq) => MetadataField(seq(i))
  }

  override def getPath(key: String): ConfPath = config.ConfPath(s"$name / $key", get(key))

  def getPath(i: Int): ConfPath = config.ConfPath(s"$name / $i", get(i))

  def isConf: Boolean = value match {
    case ConfField(_) => true
    case _ => false
  }

  override def metadata: Metadata = value match {
    case LongField(value) => new MetadataBuilder().putLong(name, value).build()
    case DoubleField(value) => new MetadataBuilder().putDouble(name, value).build()
    case BooleanField(value) => new MetadataBuilder().putBoolean(name, value).build()
    case StringField(value) => new MetadataBuilder().putString(name, value).build()
    case ConfField(value) => new MetadataBuilder().putMetadata(name, value.metadata).build()
    case LongSeqField(values) => new MetadataBuilder().putLongArray(name, values.toArray).build()
    case DoubleSeqField(values) => new MetadataBuilder().putDoubleArray(name, values.toArray).build()
    case BooleanSeqField(values) => new MetadataBuilder().putBooleanArray(name, values.toArray).build()
    case StringSeqField(values) => new MetadataBuilder().putStringArray(name, values.toArray).build()
    case ConfSeqField(values) => new MetadataBuilder().putMetadataArray(name, values.map(_.metadata).toArray).build()
  }

  override def getType(key: String): MetadataFieldType = keyTypes(key)
  
  def getLong: Long = if (value.fieldType == LongFieldType) {
    value.asInstanceOf[LongField].value
  } else {
    throw new IllegalArgumentException(s"$this is not a path to a Long")
  }

  def getDouble: Double = if (value.fieldType == DoubleFieldType) {
    value.asInstanceOf[DoubleField].value
  } else {
    throw new IllegalArgumentException(s"$this is not a path to a Double")
  }

  def getBoolean: Boolean = if (value.fieldType == BooleanFieldType) {
    value.asInstanceOf[BooleanField].value
  } else {
    throw new IllegalArgumentException(s"$this is not a path to a Boolean")
  }

  def getString: String = if (value.fieldType == StringFieldType) {
    value.asInstanceOf[StringField].value
  } else {
    throw new IllegalArgumentException(s"$this is not a path to a String")
  }

  def getConf: Configuration = if (value.fieldType == ConfFieldType) {
    Configuration(value.asInstanceOf[ConfField].value)
  } else {
    throw new IllegalArgumentException(s"$this is not a path to a Conf")
  }
}