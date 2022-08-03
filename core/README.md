# wisecube configuration and graph building

## Configuration

[Configuration example](./src/main/scala/com/wisecube/orpheus/config/README.md)

#### RDF data

If the graph data comes in an RDF format then only minimal transformation will be required at this stage. This data 
should be parsed into tables with the Orpheus schema. The fusion step is where the IRIs, literals, etc. will be mapped 
to the ultimate schema. 



#### Other Graph Formats

This data should be treated as structured data.

### Structured

There are two main concerns structured data - quality and complexity. Of course, general data quality is always 
important in data engineering, here we are talking about a specific kind of data quality. The kind of data quality we 
are concerned with is completeness and consistency. What fields are null? In what formats are different data types 
stored (e.g. dates, floating point, booleans). Complexity is the other ingredient we must manage. Transforming a 30 
column CSV into a set of triples is very different from a database with dozens of tables in 3rd normal form. 

Example 4. pipeline for extracting

```scala
import com.wisecube.orpheus.enrichment.transformers.{Joiner, ValueBuilder}
import com.wisecube.orpheus.config.Configuration
import com.wisecube.orpheus.config.graph.URIGraphConf
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

### Text

In order to add text into a graph, we must extract the information we are interested into a structured format. This is 
where NLP comes in. This library is not an NLP library, which is why there is an abstraction layer. The idea is that 
the information is extracted into a structured form, so that the downstream process does not need to know what engine 
was used for NLP.

## Fusion

### Custom transformation

### Mapping to schema

## Querying

### SPARQL

### Indexes

### Custom insights