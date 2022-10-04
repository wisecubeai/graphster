package com.graphster.orpheus.text.sparknlp.config.annotation

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.types.MetadataField
import com.graphster.orpheus.text.config.annotation._
import com.graphster.orpheus.text.sparknlp.config.annotation.JSLNamedEntityConf.annotation2row
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

case class JSLFinishedNamedEntityConf(column: String, typeColumn: String, kwargs: Configuration = Configuration.empty)
  extends NamedEntityConf(JSLNamedEntityConf.AnnotationType, kwargs.add(
    JSLAnnotation.ColumnKey -> MetadataField(column),
    JSLFinishedNamedEntityConf.TypeColumnKey -> MetadataField(typeColumn)
  )) with JSLAnnotation {
  override protected val defaultName: String = s"${column}_${JSLNamedEntityConf.AnnotationType}"

  override def toColumn: Column = JSLNamedEntityConf.anno2rowUDF(sf.col(column)).as(name, metadata)
}
object JSLFinishedNamedEntityConf extends NamedEntityConfBuilder with JSLAnnotationBuilder {
  val TypeColumnKey = "entity_type"

  override val AnnotationType: String = "jsl_named_entity"

  override def getEntityName(anno: Row): String = anno.getString(0)

  override def getEntityType(anno: Row): String = anno.getMap[String, String](1)("entity")

  override def anno2rowUDF: UserDefinedFunction = {
    val spark = SparkSession.builder.getOrCreate()
    spark.udf.register("jsl_ner2row", (annotation2row _).andThen(NamedEntityRow.apply))
  }

  override def apply(config: Configuration): JSLNamedEntityConf = {
    val column = config.getString(JSLAnnotation.ColumnKey)
    val kwargs = config.remove(JSLAnnotation.ColumnKey)
    new JSLNamedEntityConf(column, kwargs)
  }
}

