import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.graph._
import com.graphster.orpheus.config.table.{ColumnValueConf, ConcatValueConf, FallbackValueConf, StringValueConf}
import com.graphster.orpheus.config.types.MetadataField

val typeConfig = Configuration(
  "predicate" -> MetadataField(URIGraphConf("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")),
  "trial" -> MetadataField(URIGraphConf("http://schema.org/MedicalTrial")),
  "condition" -> MetadataField(URIGraphConf("http://schema.org/MedicalCondition")),
  "intervention" -> MetadataField(URIGraphConf("http://schema.org/MedicalProcedure")),
)

val nctURIConfig = URIGraphConf(ConcatValueConf(Seq(
  StringValueConf("http://clinicaltrials.gov/ct2/show/"),
  ColumnValueConf("nct_id")
)))

val meshConfig = Configuration(
  "label_uri" -> MetadataField(URIGraphConf("http://www.w3.org/2000/01/rdf-schema#label"))
)

val nameConfig = LangLiteralGraphConf(ColumnValueConf("name"), "en")

val titleConfig = Configuration(
  "predicate" -> MetadataField(URIGraphConf("http://schema.org/title")),
  "official_title" -> MetadataField(LangLiteralGraphConf(ColumnValueConf("official_title"), "en")),
  "brief_title" -> MetadataField(LangLiteralGraphConf(ColumnValueConf("brief_title"), "en")),
)

val dateConfig = Configuration(
  "predicate" -> MetadataField(URIGraphConf("http://schema.org/startDate")),
  "date" -> MetadataField(DataLiteralGraphConf(ColumnValueConf("DATE_FORMAT(study_first_submitted_date, 'yyyy-MM-dd')"), "date"))
)

val conditionConfig = Configuration(
  "predicate" -> MetadataField(URIGraphConf("http://schema.org/healthCondition")),
  "uri" -> MetadataField(URIGraphConf(FallbackValueConf(
    Seq(ColumnValueConf("meshid")),
    ConcatValueConf(Seq(
      StringValueConf("http://wisecube.com/condition#AACT"),
      ColumnValueConf("id"),
    ))))))

val interventionConfig = Configuration(
  "predicate" -> MetadataField(URIGraphConf("http://schema.org/studySubject")),
  "uri" -> MetadataField(URIGraphConf(FallbackValueConf(
    Seq(ColumnValueConf("meshid")),
    ConcatValueConf(Seq(
      StringValueConf("http://wisecube.com/intervention#AACT"),
      ColumnValueConf("id"),
    ))))))

val config = Configuration(
  "types" -> MetadataField(typeConfig),
  "mesh" -> MetadataField(meshConfig),
  "nct" -> MetadataField(Configuration(
    "name" -> MetadataField(nameConfig),
    "nct_uri" -> MetadataField(nctURIConfig),
    "title" -> MetadataField(titleConfig),
    "date" -> MetadataField(dateConfig),
    "condition" -> MetadataField(conditionConfig),
    "intervention" -> MetadataField(interventionConfig)
  )))

println(config.yaml)