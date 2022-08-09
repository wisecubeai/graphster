```scala
import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.graph.URIGraphConf
import com.wisecube.orpheus.enrichment.transformers.{Joiner, ValueBuilder}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.SQLTransformer
import org.apache.spark.sql.{DataFrame, SparkSession}

val spark: SparkSession = SparkSession.builder.getOrCreate()

val data: DataFrame = spark.read.csv("...")

val config = Configuration.load("...")

val pipeline: PipelineModel = new Pipeline().setStages(Array(
  new Joiner().setReferenceTable("...").setValueMetas(Array("left.a", "right.a", "...")).setOutputCols(Array("right.b", "right.c")),
  new SQLTransformer().setStatement("..."),
  new ValueBuilder().setValueMeta(config.get[URIGraphConf.Meta]("qid2uri").uri)
)).fit(data)

val transformed = pipeline.transform(data)
transformed.write.saveAsTable("...")
pipeline.save("...")
```