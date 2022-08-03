package com.wisecube.orpheus.text.sparknlp.config.pipeline

import com.johnsnowlabs.nlp.SparkNLP
import com.johnsnowlabs.nlp.pretrained.{PretrainedPipeline, ResourceDownloader}
import com.wisecube.orpheus.config.Configuration
import com.wisecube.orpheus.config.table.ColumnValueConf
import com.wisecube.orpheus.text.config.annotation.AnnotationConf
import com.wisecube.orpheus.text.config.pipeline.{PipelineConf, PipelineConfBuilder}
import com.wisecube.orpheus.text.sparknlp.config.annotation.{JSLAnnotation, JSLNamedEntityConf}
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

  def main(args: Array[String]): Unit = {
    val spark = SparkNLP.start()
    import spark.implicits._

    val text =
      """
        |Diamond-Blackfan anemia (DBA) is a congenital hypoproliferative anemia that generally
        |presents in infancy. The mainstays of treatment, prednisone and transfusion therapy, have
        |long-term toxicity in many patients, and bone marrow transplantation with an HLA-matched
        |donor is an option for only a minority of patients. Most importantly, patients with DBA have
        |an increased risk of progression of myelodysplastic syndrome, leukemia, and aplastic anemia
        |compared to the general population.~The characterization of potentially mutated genes in DBA
        |is an area of active research, and at least one mutation present in about one-fourth of DBA
        |patients may cause disease due to decreased production of a ribosomal protein. This finding
        |raises the possibility that the disease, at least in some patients, may be correctable by
        |genetic therapy, by which a normal copy of the mutated gene can be introduced into the stem
        |cells which give rise to red cells.~It is therefore of interest to identify any particular
        |characteristics of DBA patients which might delay or hinder the application of gene therapy
        |to their disease. This pilot study of 15 patients is designed to evaluate: 1) the CD34+ cell
        |mobilization response to administration of standard doses of granulocyte-colony stimulating
        |factor (G-CSF), 2) the potential for stem cells from DBA patients to be collected by large
        |volume leukapheresis of subjects who have been given G-CSF, and 3) the ability of these G-CSF
        |mobilized cells to be transduced with vectors being developed for gene therapy applications.
        |Outcome parameters to be monitored are the mobilization response to G-CSF, the safety profile
        |and tolerance of G-CSF and leukapheresis, and the efficiency of transduction of DBA stem
        |cells with standard gene therapy vectors. Effectiveness will be gauged by historical
        |comparison of these parameters to normal healthy age-matched volunteer.~It is important to
        |point out that there is no therapeutic intent to the majority of this protocol or direct
        |benefit for enrolled patients. We do plan, however, to cryopreserve the remainder of the
        |mobilized cells collected by apheresis for possible autologous transplantation in the event
        |of the patient's progression to leukemia of bone marrow failure in the future.
        |""".stripMargin.replace("\n", "")

    val df = Seq((1, text)).toDF("id", "text")

    df.show()

    val ppc = PretrainedPipelineConf(
      Map("entities" -> JSLNamedEntityConf(
        "named_entity",
        "entity"
      )),
      "explain_document_dl", "en"
    )

    val pipeline = ppc.loadPipeline

    val tx = pipeline.transform(df).select($"id", $"text", functions.explode($"entities").as("entity"))

    tx.show()

    tx.printSchema()

    tx.select(
      tx("id"),
      tx("text"),
      ppc.outputs("entities").toColumn
    ).show()
  }
}