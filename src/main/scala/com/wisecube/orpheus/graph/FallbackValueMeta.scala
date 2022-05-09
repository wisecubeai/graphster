package com.wisecube.orpheus.graph

import com.wisecube.orpheus.graph.FallbackValueMeta.finalValKey
import org.apache.spark.sql.{Column, functions => sf}
import org.apache.spark.sql.types.Metadata

case class FallbackValueMeta(name: String, columns: Seq[ValueMeta], finalVal: AtomicValueMeta) extends ValueMeta {
  override protected val builder: ValueMetaBuilder[_ >: FallbackValueMeta.this.type] = FallbackValueMeta

  override def toMetadata: Metadata = {
    assert(columns.nonEmpty)
    val builder = buildMetadata.putMetadataArray(FallbackValueMeta.columnsKey, columns.map(_.toMetadata).toArray)
    if (!finalVal.isEmpty) {
      builder.putMetadata(FallbackValueMeta.finalValKey, finalVal.toMetadata)
    }
    builder.build()
  }

  override def toColumn: Column = sf.coalesce(columns.map(_.toColumn) :+ finalVal.toColumn: _*).as(name, toMetadata)
}

object FallbackValueMeta extends ValueMetaBuilder[FallbackValueMeta] {
  val columnsKey = "columns"
  val finalValKey = "finalVal"

  def apply(columns: Seq[ValueMeta], finalVal: AtomicValueMeta): FallbackValueMeta = {
    assert(columns.nonEmpty)
    apply(columns.head.name, columns, finalVal)
  }

  def apply(name: String, columns: Seq[ValueMeta]): FallbackValueMeta = apply(name, columns, EmptyValueMeta())

  def apply(columns: Seq[ValueMeta]): FallbackValueMeta = apply(columns, EmptyValueMeta())

  override def fromMetadata(metadata: Metadata): FallbackValueMeta = if (metadata.contains(finalValKey)) {
    FallbackValueMeta(
      name = metadata.getString(ValueMeta.nameKey),
      columns = metadata.getMetadataArray(columnsKey).map(ValueMeta.fromMetadata),
      finalVal = LiteralValueMeta.fromMetadata(metadata.getMetadata(finalValKey))
    )
  } else {
    FallbackValueMeta(
      name = metadata.getString(ValueMeta.nameKey),
      columns = metadata.getMetadataArray(columnsKey).map(ValueMeta.fromMetadata)
    )
  }
}