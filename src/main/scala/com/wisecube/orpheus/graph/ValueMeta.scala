package com.wisecube.orpheus.graph

import org.apache.spark.sql.Column
import org.apache.spark.sql.types.{Metadata, MetadataBuilder}

trait ValueMeta {
  protected val builder: ValueMetaBuilder[_ >: this.type] = ValueMeta
  val name: String
  val isEmpty: Boolean = false
  def toColumn: Column
  def toMetadata: Metadata

  protected def buildMetadata: MetadataBuilder = new MetadataBuilder()
    .putString(ValueMeta.typeKey, builder.metaType)
    .putString(ValueMeta.nameKey, name)
}

object ValueMeta extends ValueMetaBuilder[ValueMeta] {
  val typeKey = "type"
  val nameKey = "name"

  def fromMetadata(metadata: Metadata): ValueMeta =
    metadata.getString(typeKey) match {
      case LiteralValueMeta.metaType => LiteralValueMeta.fromMetadata(metadata)
      case ColumnValueMeta.metaType => ColumnValueMeta.fromMetadata(metadata)
      case EmptyValueMeta.metaType => EmptyValueMeta.fromMetadata(metadata)
      case FallbackValueMeta.metaType => FallbackValueMeta.fromMetadata(metadata)
      case ConcatValueMeta.metaType => ConcatValueMeta.fromMetadata(metadata)
      case URIElement.metaType => URIElement.fromMetadata(metadata)
      case DataLiteralElement.metaType => DataLiteralElement.fromMetadata(metadata)
      case LangLiteralElement.metaType => LangLiteralElement.fromMetadata(metadata)
      case BlankElement.metaType => BlankElement.fromMetadata(metadata)
      case TripleElement.metaType => TripleElement.fromMetadata(metadata)
      case _ => throw new IllegalArgumentException(s"Unrecognized ValueMeta type - $metaType")
    }
}

trait ValueMetaBuilder[T <: ValueMeta] {
  val metaType: String = this.getClass.getSimpleName
  def fromMetadata(metadata: Metadata): T
}

trait AtomicValueMeta extends ValueMeta