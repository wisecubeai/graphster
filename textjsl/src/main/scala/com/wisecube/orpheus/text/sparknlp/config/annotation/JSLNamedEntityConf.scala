package com.wisecube.orpheus.text.sparknlp.config.annotation

import com.wisecube.orpheus.config.types.MetadataField
import com.wisecube.orpheus.config.{AtomicValue, Configuration, ValueConf}
import com.wisecube.orpheus.text.config.annotation._
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

case class JSLNamedEntityConf(name: String, column: String)
  extends NamedEntityConf(JSLNamedEntityConf.AnnotationType, Configuration(
    ValueConf.NameKey -> MetadataField(name),
    JSLAnnotation.ColumnKey -> MetadataField(column)
  )) with JSLAnnotation {
  override def toColumn: Column = JSLNamedEntityConf.anno2rowUDF(sf.col(column)).as(name, metadata)
}

object JSLNamedEntityConf extends NamedEntityConfBuilder with JSLAnnotationBuilder {

  override val AnnotationType: String = "jsl_named_entity"

  override def getEntityName(anno: Row): String = anno.getString(3)

  override def getEntityType(anno: Row): String = anno.getMap[String, String](4)("entity")

  override def anno2rowUDF: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("jsl_ner2row", (annotation2row _).andThen(NamedEntityRow.apply))
  }

  override def apply(config: Configuration): JSLNamedEntityConf =
    new JSLNamedEntityConf(
      config.getString(ValueConf.NameKey),
      config.getString(JSLAnnotation.ColumnKey)
    )
}
