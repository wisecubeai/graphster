package com.wisecube.orpheus.text.config.annotation

import com.wisecube.orpheus.config.Configuration
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{StringType, StructField, StructType}

abstract class RelationConf(annoType: String, config: Configuration) extends AnnotationConf(annoType, config)

abstract class RelationConfBuilder(linkedEntityConfBuilder: LinkedEntityConfBuilder) extends AnnotationConfBuilder {
  val SubjectKey: String = "subject"
  val RelationKey: String = "relation"
  val ObjectKey: String = "object"

  override val innerSchema: StructType = StructType(Seq(
    StructField(SubjectKey, linkedEntityConfBuilder.schema),
    StructField(RelationKey, StringType),
    StructField(ObjectKey, linkedEntityConfBuilder.schema),
  ))

  override def value2row(anno: Row): Row = new GenericRowWithSchema(
    Array(
      linkedEntityConfBuilder.value2row(getSubject(anno)),
      getRelation(anno),
      linkedEntityConfBuilder.value2row(getObject(anno)),
    ),
    innerSchema
  )

  def getSubject(anno: Row): Row
  def getRelation(anno: Row): String
  def getObject(anno: Row): Row
}