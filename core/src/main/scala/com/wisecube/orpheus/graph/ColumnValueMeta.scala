package com.wisecube.orpheus.graph

import org.apache.spark.sql.{ Column, functions => sf }
import org.apache.spark.sql.types.Metadata

case class ColumnValueMeta(name: String, column: String) extends AtomicValueMeta {
  override protected val builder: ValueMetaBuilder[_ >: ColumnValueMeta.this.type] = ColumnValueMeta

  override def toMetadata: Metadata = {
    assert(column != null)
    buildMetadata.putString(ColumnValueMeta.columnKey, column).build()
  }

  override def toColumn: Column = sf.col(column).cast("string").as(name, toMetadata)
}

object ColumnValueMeta extends ValueMetaBuilder[ColumnValueMeta] {
  val columnKey = "column"

  def apply(column: String): ColumnValueMeta = apply(column, column)

  override def fromMetadata(metadata: Metadata): ColumnValueMeta = ColumnValueMeta(
    name = metadata.getString(ValueMeta.nameKey),
    column = metadata.getString(columnKey)
  )
}