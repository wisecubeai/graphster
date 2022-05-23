package com.wisecube.orpheus.graph

import org.apache.spark.sql.{ Column, functions => sf }
import org.apache.spark.sql.types.Metadata

case class ConcatValueMeta(name: String, values: Seq[ValueMeta]) extends ValueMeta {
  override protected val builder: ValueMetaBuilder[_ >: ConcatValueMeta.this.type] = ConcatValueMeta

  override def toMetadata: Metadata = {
    assert(values.nonEmpty)
    buildMetadata.putMetadataArray(ConcatValueMeta.valuesKey, values.map(_.toMetadata).toArray).build()
  }

  override def toColumn: Column = sf.concat(values.map(_.toColumn): _*).as(name, toMetadata)
}

object ConcatValueMeta extends ValueMetaBuilder[ConcatValueMeta] {
  val valuesKey = "values"

  def apply(values: Seq[ValueMeta]): ConcatValueMeta = {
    assert(values.nonEmpty)
    apply(values.head.name, values)
  }

  override def fromMetadata(metadata: Metadata): ConcatValueMeta = ConcatValueMeta(
    name = metadata.getString(ValueMeta.nameKey),
    values = metadata.getMetadataArray(valuesKey).map(ValueMeta.fromMetadata)
  )
}