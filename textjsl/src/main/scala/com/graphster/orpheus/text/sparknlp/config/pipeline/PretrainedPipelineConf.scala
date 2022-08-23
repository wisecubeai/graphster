package com.graphster.orpheus.text.sparknlp.config.pipeline

import com.graphster.orpheus.text.config.annotation.AnnotationConf
import com.johnsnowlabs.nlp.SparkNLP
import com.johnsnowlabs.nlp.pretrained.{PretrainedPipeline, ResourceDownloader}
import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.table.ColumnValueConf
import com.graphster.orpheus.text.config.annotation.AnnotationConf
import com.graphster.orpheus.text.config.pipeline.{PipelineConf, PipelineConfBuilder}
import com.graphster.orpheus.text.sparknlp.config.annotation.{JSLAnnotation, JSLNamedEntityConf}
import org.apache.spark.ml.{PipelineModel, Transformer}
import org.apache.spark.sql.functions

case class PretrainedPipelineConf(
  outputs: Map[String, AnnotationConf],
  downloadName: String,
  lang: String = "en",
  source: String = ResourceDownloader.publicLoc,
  parseEmbeddingsVectors: Boolean = false,
  diskLocation: Option[String] = None,
) extends PipelineConf {
  lazy val pretrainedPipeline: PretrainedPipeline =
    new PretrainedPipeline(downloadName, lang, source, parseEmbeddingsVectors, diskLocation)

  lazy val model: PipelineModel = pretrainedPipeline.model

  override def loadPipeline: Transformer = model
}

object PretrainedPipelineConf extends PipelineConfBuilder[PretrainedPipelineConf] {
  val OutputsKey: String = "outputs"
  val DownloadNameKey: String = "downloadName"
  val LangKey: String = "lang"
  val SourceKey: String = "source"
  val ParseEmbeddingsVectorsKey: String = "parseEmbeddingsVectors"
  val DiskLocationKey: String = "diskLocation"

  override def apply(config: Configuration): PretrainedPipelineConf = {
    var pipelineConf = new PretrainedPipelineConf(
      config.getConf(OutputsKey).keys.map(k => k -> JSLAnnotation(config.getConf(OutputsKey).getConf(k))).toMap,
      config.getString(DownloadNameKey)
    )
    if (config.hasString(LangKey)) {
      pipelineConf = pipelineConf.copy(lang = config.getString(LangKey))
    }
    if (config.hasString(SourceKey)) {
      pipelineConf = pipelineConf.copy(source = config.getString(SourceKey))
    }
    if (config.hasString(ParseEmbeddingsVectorsKey)) {
      pipelineConf = pipelineConf.copy(parseEmbeddingsVectors = config.getBoolean(ParseEmbeddingsVectorsKey))
    }
    if (config.hasString(DiskLocationKey)) {
      pipelineConf = pipelineConf.copy(diskLocation = Some(config.getString(DiskLocationKey)))
    }
    pipelineConf
  }
}