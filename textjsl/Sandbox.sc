import com.graphster.orpheus.config.graph.{LangLiteralGraphConf, TripleGraphConf, URIGraphConf}
import com.graphster.orpheus.config.table.{ColumnValueConf, StringValueConf}
import com.graphster.orpheus.text.fusion.NamedEntityReference
import com.graphster.orpheus.text.sparknlp.config.annotation.{JSLFinishedNamedEntityConf, JSLNamedEntityConf}
import com.graphster.orpheus.text.sparknlp.config.pipeline.PretrainedPipelineConf
import com.johnsnowlabs.nlp.SparkNLP
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.SQLTransformer

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

val df = Seq(("http://www.example.org/1", text)).toDF("id", "text")

df.show()

val ppc = PretrainedPipelineConf(
  Map("entities" -> JSLNamedEntityConf(
    "entity"
  )),
  "nerdl_fewnerd_100d_pipeline", "en"
)

val postNERTransformer = new SQLTransformer().setStatement(
  """
    |SELECT EXPLODE(TRANSFORM(
    | SEQUENCE(0, SIZE(finished_ner_chunk)-1),
    | ix -> NAMED_STRUCT(
    |   "name", finished_ner_chunk[ix],
    |   "info", MAP(
    |     "entity", finished_ner_chunk_metadata[ix*4],
    |     "confidence", finished_ner_chunk_metadata[ix*4+3]
    |   )
    | )
    |)) AS entity
    |FROM __THIS__
    |""".stripMargin)

val nerReference = new NamedEntityReference()
  .setNamedEntityConf(JSLFinishedNamedEntityConf("entity", "ner"))
  .setOutputCol("named_entities")
  .addTripleMeta(new TripleGraphConf(
    URIGraphConf(ColumnValueConf("id")),
    URIGraphConf(StringValueConf("https://schema.org/mentions")),
    LangLiteralGraphConf(ColumnValueConf("named_entities.name"), "en")
  ))

nerReference.transform(postNERTransformer.transform(ppc.loadPipeline.transform(df))).show()

val textPipeline = new Pipeline().setStages(Array(
  ppc.loadPipeline,
  postNERTransformer,
  nerReference,
//  new TripleExtractor()
)).fit(df)

//textPipeline.transform(df).show()