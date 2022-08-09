package com.graphster.orpheus.text.config.annotation

import com.graphster.orpheus.config.Configuration
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{StringType, StructField, StructType}

abstract class LinkedEntityConf(annoType: String, config: Configuration) extends NamedEntityConf(annoType, config)

trait LinkedEntityConfBuilder extends NamedEntityConfBuilder {
  val EntityIdKey: String = "entityId"
  val IdSourceKey: String = "idSource"

  override val innerSchema: StructType = StructType(Seq(
    StructField(EntityNameKey, StringType),
    StructField(EntityTypeKey, StringType),
    StructField(EntityIdKey, StringType),
    StructField(IdSourceKey, StringType),
  ))

  override def value2row(anno: Row): Row = new GenericRowWithSchema(
    Array(getEntityName(anno), getEntityType(anno), getEntityId(anno), getIdSource(anno)),
    innerSchema
  )

  def getEntityId(anno: Row): String
  def getIdSource(anno: Row): String
}

case class LinkedEntityRow(name: String, annotationType: String, location: Location, value: LinkedEntity) {
  def apply(row: Row): LinkedEntityRow = row match {
    case Row(
    `type`: String,
    annotationType: String,
    Row(start: Int, end: Int),
    Row(entityName: String, entityType: String, entityId: String, idSource: String),
    ) =>
      LinkedEntityRow(
        `type`,
        annotationType,
        Location(start, end),
        LinkedEntity(entityName, entityType, entityId, idSource)
      )
    case _ => throw new IllegalArgumentException(s"wrong row shape - $row")
  }
}
case class LinkedEntity(entityName: String, entityType: String, entityId: String, idSource: String)