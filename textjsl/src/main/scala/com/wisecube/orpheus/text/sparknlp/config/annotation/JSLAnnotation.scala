package com.wisecube.orpheus.text.sparknlp.config.annotation

import com.wisecube.orpheus.config.types.{ConfFieldType, MetadataField, StringFieldType}
import com.wisecube.orpheus.config.{AtomicValue, Configuration, ValueConf, types}
import com.wisecube.orpheus.text.config.annotation.{AnnotationConf, AnnotationConfBuilder, Location}
import org.apache.spark.sql.Row

trait JSLAnnotation {
  self: AnnotationConf =>
  val column: String

  override def keys: Set[String] = Set(ValueConf.NameKey, JSLAnnotation.ColumnKey)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    JSLAnnotation.ColumnKey -> ConfFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case JSLAnnotation.ColumnKey => MetadataField(column)
  }
}

object JSLAnnotation {
  val ColumnKey: String = "column"

  def apply(config: Configuration): AnnotationConf with JSLAnnotation =
    config.getString(AnnotationConfBuilder.AnnotationTypeKey) match {
      case JSLNamedEntityConf.AnnotationType => JSLNamedEntityConf(config)
    }
}

trait JSLAnnotationBuilder {
  self: AnnotationConfBuilder =>

  def getLocation(anno: Row): Location = Location(anno.getInt(1), anno.getInt(2))
}