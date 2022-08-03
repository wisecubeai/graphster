package com.wisecube.orpheus.config

import com.wisecube.orpheus.config.table.{ColumnValueConf, ConcatValueConf, EmptyValueConf, FallbackValueConf, LiteralValueConf}
import com.wisecube.orpheus.config.types.MetadataField
import org.apache.spark.sql.Column
import org.apache.spark.sql.types.Metadata
import spire.ClassTag

abstract class ValueConf(config: Configuration) extends Conf {
  def this(fields: (String, MetadataField[_])*) {
    this(Configuration(fields: _*))
  }
  val name: String

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

  override def fromMetadata(metadata: Metadata): T = apply(Configuration.fromMetadata(metadata))
}

trait AtomicValue {
  self: ValueConf =>
}

object AtomicValue {
  def fromConfiguration(config: Configuration): ValueConf with AtomicValue = {
    val name = config.getString(ValueConf.NameKey)
    if (name != EmptyValueConf.Name) {
      if (config.hasString(LiteralValueConf.DatatypeKey)) {
        LiteralValueConf(config)
      } else if (config.hasString(ColumnValueConf.ColumnKey)) {
        ColumnValueConf(config)
      } else {
        throw new IllegalArgumentException(s"Unrecognized configuration - $config")
      }
    } else {
      EmptyValueConf()
    }
  }
}

