package com.graphster.orpheus.text.config.annotation

import com.graphster.orpheus.config.Configuration
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{StringType, StructField, StructType}

abstract class NamedEntityConf(annoType: String, config: Configuration) extends AnnotationConf(annoType, config)

trait NamedEntityConfBuilder extends AnnotationConfBuilder {
  val EntityNameKey: String = "entityName"
  val EntityTypeKey: String = "entityType"

  override val innerSchema: StructType = StructType(Seq(
    StructField(EntityNameKey, StringType),
    StructField(EntityTypeKey, StringType),
  ))

  override def value2row(anno: Row): Row = new GenericRowWithSchema(
    Array(getEntityName(anno), getEntityType(anno)),
    innerSchema
  )

  def getEntityName(anno: Row): String
  def getEntityType(anno: Row): String
}

case class NamedEntityRow(annotationType: String, location: Location, value: NamedEntity)

object NamedEntityRow {
  def apply(row: Row): NamedEntityRow = row match {
    case Row(
      annotationType: String,
      Location(start: Int, end: Int),
      Row(entityName: String, entityType: String)
    ) =>
      NamedEntityRow(
        annotationType,
        Location(start, end),
        NamedEntity(entityName, entityType)
      )
    case _ => throw new IllegalArgumentException(s"wrong row shape - $row")
  }
}

case class NamedEntity(entityName: String, entityType: String)