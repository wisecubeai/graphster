package com.graphster.orpheus.text.sparknlp.config.annotation

import com.graphster.orpheus.config.{Configuration, ValueConf}
import com.graphster.orpheus.text.config.annotation.{NamedEntityConfBuilder, NamedEntityRow}
import com.graphster.orpheus.config.types.MetadataField
import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf}
import com.graphster.orpheus.text.config.annotation._
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{Column, Row, SparkSession, functions => sf}

case class JSLNamedEntityConf(column: String, kwargs: Configuration = Configuration.empty)
  extends NamedEntityConf(JSLNamedEntityConf.AnnotationType, kwargs.add(
    JSLAnnotation.ColumnKey -> MetadataField(column)
  )) with JSLAnnotation {
  override protected val defaultName: String = s"${column}_${JSLNamedEntityConf.AnnotationType}"

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

  override def apply(config: Configuration): JSLNamedEntityConf = {
    val column = config.getString(JSLAnnotation.ColumnKey)
    val kwargs = config.remove(JSLAnnotation.ColumnKey)
    new JSLNamedEntityConf(column, kwargs)
  }
}
