package com.wisecube.orpheus.text.config.annotation

import com.wisecube.orpheus.config.types.MetadataField
import com.wisecube.orpheus.config.{Configuration, ValueConf, ValueConfBuilder}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.types._

abstract class AnnotationConf(annoType: String, config: Configuration)
  extends ValueConf(Configuration(config.fields + (AnnotationConfBuilder.AnnotationTypeKey -> MetadataField(annoType))))

abstract class AnnotationConfBuilder extends ValueConfBuilder[AnnotationConf] {
  val innerSchema: StructType
  val schema: StructType = StructType(Seq(
    StructField(AnnotationConfBuilder.AnnotationTypeKey, StringType),
    StructField(AnnotationConfBuilder.LocationKey, Location.schema),
    StructField(AnnotationConfBuilder.ValueKey, innerSchema)
  ))

  val AnnotationType: String

  def annotation2row(anno: Row): Row = new GenericRowWithSchema(
    Array(AnnotationType, getLocation(anno), value2row(anno)),
    schema
  )

  def value2row(anno: Row): Row

  def getLocation(anno: Row): Location

  def anno2rowUDF: UserDefinedFunction
}

object AnnotationConfBuilder {
  val AnnotationTypeKey: String = "annotationType"
  val LocationKey: String = "location"
  val ValueKey: String = "value"
}

case class Location(start: Int, end: Int)

object Location {
  val StartKey: String = "start"
  val EndKey: String = "end"
  val schema: StructType = StructType(Seq(
    StructField(StartKey, IntegerType),
    StructField(EndKey, IntegerType),
  ))
}