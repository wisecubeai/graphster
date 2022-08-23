package com.graphster.orpheus.config

import com.graphster.orpheus.config.table._
import com.graphster.orpheus.config.types.MetadataField
import org.apache.spark.sql.Column
import org.apache.spark.sql.types.Metadata
import spire.ClassTag

abstract class ValueConf(config: Configuration) extends Conf {
  def this(fields: (String, MetadataField[_])*) {
    this(Configuration(fields: _*))
  }

  protected def defaultName: String

  val kwargs: Configuration

  def name: String = if (kwargs.contains(ValueConf.NameKey)) {
    kwargs.getString(ValueConf.NameKey)
  } else {
    defaultName
  }

  def toColumn: Column

  override val metadata: Metadata = config.metadata
}

object ValueConf {
  val NameKey: String = "name"

  def fromConfiguration(config: Configuration): ValueConf = {
    if (config.hasConfSeq(ConcatValueConf.ValuesKey)) {
      ConcatValueConf(config)
    } else if (config.hasConfSeq(FallbackValueConf.ColumnsKey)) {
      FallbackValueConf(config)
    } else {
      AtomicValue.fromConfiguration(config)
    }
  }
}

abstract class ValueConfBuilder[T <: ValueConf : ClassTag] extends ConfBuilder[T] {
  def apply(config: Configuration): T

  def apply(field: (String, MetadataField[_]), fields: (String, MetadataField[_])*): T =
    apply(Configuration(fields: _*).add(field))

  override def fromMetadata(metadata: Metadata): T = apply(Configuration.fromMetadata(metadata))
}

trait AtomicValue {
  self: ValueConf =>
}

object AtomicValue {
  def fromConfiguration(config: Configuration): ValueConf with AtomicValue = {
    if (config.keys.nonEmpty) {
      if (config.hasString(LiteralValueConf.ValueKey)) {
        LiteralValueConf(config)
      } else if (config.hasString(ColumnValueConf.ColumnKey)) {
        table.ColumnValueConf(config)
      } else {
        throw new IllegalArgumentException(s"Unrecognized configuration - $config")
      }
    } else {
      EmptyValueConf()
    }
  }
}

