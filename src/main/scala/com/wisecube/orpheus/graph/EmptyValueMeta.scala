package com.wisecube.orpheus.graph

import org.apache.spark.sql.{ Column, functions => sf }
import org.apache.spark.sql.types.Metadata

case class EmptyValueMeta() extends AtomicValueMeta with NodeMeta {
  override protected val builder: ValueMetaBuilder[_ >: EmptyValueMeta.this.type] = EmptyValueMeta

  override val name: String = EmptyValueMeta.name

  override def toMetadata: Metadata = buildMetadata.build()

  override def toColumn: Column = sf.lit(null).cast("string").as(name)

  override val isEmpty: Boolean = true
}

object EmptyValueMeta extends ValueMetaBuilder[AtomicValueMeta] {

  val name: String = "_EMPTY_"

  override def fromMetadata(metadata: Metadata): EmptyValueMeta = EmptyValueMeta()
}