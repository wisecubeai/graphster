package com.wisecube.orpheus.graph

import org.apache.spark.sql.{ Column, functions => sf }
import org.apache.spark.sql.types.Metadata

case class LiteralValueMeta(name: String, value: String) extends AtomicValueMeta {
  if (name.isBlank && value.isBlank) {
    throw new IllegalArgumentException("either name or value must be non-empty/non-blank")
  }

  override protected val builder: ValueMetaBuilder[_ >: LiteralValueMeta.this.type] = LiteralValueMeta

  override def toMetadata: Metadata = {
    require(value != null)
    buildMetadata.putString(LiteralValueMeta.valueKey, value).build()
  }

  override def toColumn: Column = sf.lit(value).as(name, toMetadata)
}

object LiteralValueMeta extends ValueMetaBuilder[LiteralValueMeta] {
  val valueKey = "value"

  def apply(value: String): LiteralValueMeta = apply(s"`$value`", value)

  override def fromMetadata(metadata: Metadata): LiteralValueMeta =
    LiteralValueMeta(name = metadata.getString(ValueMeta.nameKey), value = metadata.getString(valueKey))
}