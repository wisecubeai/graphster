package com.graphster.orpheus.text.config.pipeline

import com.graphster.orpheus.text.config.annotation.AnnotationConf
import com.graphster.orpheus.config.types.{ConfFieldType, MetadataField, MetadataFieldType}
import com.graphster.orpheus.config.{Conf, ConfBuilder, Configuration}
import com.graphster.orpheus.text.config.annotation.AnnotationConf
import org.apache.spark.ml.Transformer
import org.apache.spark.sql.types.Metadata

import scala.reflect.ClassTag

abstract class PipelineConf(config: Configuration) extends Conf {
  def this(fields: (String, MetadataField[_])*) {
    this(Configuration(fields: _*))
  }

  def loadPipeline: Transformer

  def outputs: Map[String, AnnotationConf]

  def keys: Set[String] = outputs.keySet

  def keyTypes: Map[String, MetadataFieldType] = outputs.mapValues(_ => ConfFieldType)

  def get(key: String): MetadataField[_] = MetadataField(outputs(key))

  override val metadata: Metadata = config.metadata
}

abstract class PipelineConfBuilder[T <: PipelineConf : ClassTag] extends ConfBuilder[PipelineConf] {
  def apply(config: Configuration): T

  override def fromMetadata(metadata: Metadata): T = apply(Configuration.fromMetadata(metadata))
}
