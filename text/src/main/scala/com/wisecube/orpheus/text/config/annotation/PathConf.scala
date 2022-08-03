package com.wisecube.orpheus.text.config.annotation

import com.wisecube.orpheus.config.Configuration
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{ArrayType, StringType, StructField, StructType}

abstract class PathConf(annoType: String, config: Configuration) extends AnnotationConf(annoType, config)

abstract class PathConfBuilder(linkedEntityConfBuilder: LinkedEntityConfBuilder) extends AnnotationConfBuilder {
  val StartEntityKey: String = "startEntity"
  val PathKey: String = "path"
  val EndEntityKey: String = "endEntity"

  override val innerSchema: StructType = StructType(Seq(
    StructField(StartEntityKey, linkedEntityConfBuilder.schema),
    StructField(PathKey, ArrayType(StringType)),
    StructField(EndEntityKey, linkedEntityConfBuilder.schema),
  ))

  override def value2row(anno: Row): Row = new GenericRowWithSchema(
    Array(
      linkedEntityConfBuilder.value2row(getStartEntity(anno)),
      getPath(anno),
      linkedEntityConfBuilder.value2row(getEndEntity(anno)),
    ),
    innerSchema
  )

  def getStartEntity(anno: Row): Row
  def getPath(anno: Row): Array[String]
  def getEndEntity(anno: Row): Row
}

case class PathRow(startEntity: LinkedEntityRow, path: Seq[String], endEntity: LinkedEntityRow)

object PathRow {
  def apply(row: Row): PathRow = row match {
    case Row(
      startEntity: LinkedEntityRow,
      path: Seq[String],
      endEntity: LinkedEntityRow
    ) => new PathRow(startEntity, path, endEntity)
    case _ => throw new IllegalArgumentException(s"wrong row shape - $row")
  }
}