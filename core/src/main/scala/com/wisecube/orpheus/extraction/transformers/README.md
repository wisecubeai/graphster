
```scala
import com.wisecube.orpheus.extraction.transformers.{Joiner, ValueBuilder}
import com.wisecube.orpheus.graph.{Configuration, URIElement}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.SQLTransformer
import org.apache.spark.sql.{DataFrame, SparkSession}

val spark: SparkSession = SparkSession.builder.getOrCreate()

val data: DataFrame = spark.read.csv("...")

val config = Configuration.load("...")

val pipeline: PipelineModel = new Pipeline().setStages(Array(
  new Joiner().setReferenceTable("...").setValueMetas(Array("left.a", "right.a", "...")).setOutputCols(Array("right.b", "right.c")),
  new SQLTransformer().setStatement("..."),
  new ValueBuilder().setValueMeta(config.get[URIElement.Meta]("qid2uri").uri)
)).fit(data)

val transformed = pipeline.transform(data)
transformed.write.saveAsTable("...")
pipeline.save("...")
```