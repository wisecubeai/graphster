# wisecube

`wisecube` is an open-source spark-based library for scalable, end-to-end knowledge graph construction and querying from unstructured and structured source data. The Wisecube library takes a collection of documents, extracts mentions and relations to populate a raw knowledge graph, links mentions to entities in Wikidata, and then enriches the knowledge graph with facts from Wikidata. Once the knowledge graph is built, Wisecube can also help natively query the knowledge graph using [`SPARQL`](https://en.wikipedia.org/wiki/SPARQL/).

See [`wisecube.org`](http://www.wisecube.org/) for an overview of the library.

 
This README provies instructions on how to use the library in your own project.

# Setup

Clone [wisecube](https://github.com/thecloudcircle/wisecube):

```
git clone https://github.com/thecloudcircle/wisecube.git
```

## Configuration

```json
{
  "qid2uri": {
    "type": "URIElement",
    "name": "wikidata_uri",
    "uri": {
      "type": "ConcatValueMeta",
      "name": "wikidata_uri",
      "values": [
        {
          "type": "LiteralValueMeta",
          "name": "wikidata_ns",
          "value": ""
        },
        {}
      ]
    }
  }
}
```

## Data Sources

In order to build a knowledge graph you must be able to combine data from other graphs, structured data, and text data. 

1. Graph data: e.g. OWL files, RDF files
2. Structured data: e.g. CSV files, RDBMS database dumps
3. Text data: e.g. Document corpus, text fields in other kinds of data (>= 50 words on average)

The difficulty is that these data sets require different kinds of processing. The idea here is to transform all the 
data into structured data that will then be transformed into a graph-friendly format. This breaks this complex 
processing into three phases.

1. Extraction: where we extract the facts and properties that we are interested from the raw source data
2. Fusion: where we transform the extracted information into a graph-friendly format with a common schema
3. Querying: where we search the data

Now that we have broken up this complex process into more manageable parts, let's look at how this library helps 
enable graph construction.

## Extraction

The extraction phase will generally by the most source-specific part of ingestion. In this part logic necessary for 
transforming the data into a format fusing into the ultimate graph's schema.

### Graph

#### RDF data

If the graph data comes in an RDF format then only minimal transformation will be required at this stage. This data 
should be parsed into tables with the Orpheus schema. The fusion step is where the IRIs, literals, etc. will be mapped 
to the ultimate schema. 

Example 1.
```scala
import org.apache.spark.sql.{DataFrame, SparkSession}

val spark: SparkSession = ???

val rdfData: DataFrame = spark.read.format("...").load("...")
rdfData.write.saveAsTable("...")
```

#### Other Graph Formats

This data should be treated as structured data.

### Structured

There are two main concerns structured data - quality and complexity. Of course, general data quality is always 
important in data engineering, here we are talking about a specific kind of data quality. The kind of data quality we 
are concerned with is completeness and consistency. What fields are null? In what formats are different data types 
stored (e.g. dates, floating point, booleans). Complexity is the other ingredient we must manage. Transforming a 30 
column CSV into a set of triples is very different from a database with dozens of tables in 3rd normal form. 

Example 2.

```scala
import com.wisecube.orpheus.extraction.transformers.{Joiner, ValueBuilder}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.feature.SQLTransformer
import org.apache.spark.sql.{DataFrame, SparkSession}

val spark: SparkSession = ???

val data: DataFrame = spark.read.csv("...")

val pipeline: PipelineModel = new Pipeline().setStages(Array(
  new Joiner().setReferenceTable("...").setValueMetas(Array("left.a", "right.a", "...")).setOutputCols(Array("right.b", "right.c")),
  new SQLTransformer().setStatement("..."),
  new ValueBuilder().setValueMeta(???)
)).fit(data)

val transformed = pipeline.transform(data)
transformed.write.saveAsTable("...")
pipeline.save("...")

```

### Text

In order to add text into a graph, we must extract the information we are interested into a structured format. This is 
where NLP comes in. This library is not an NLP library, which is why there is an abstraction layer. The idea is that 

## Fusion

### Custom transformation

### Mapping to schema

## Querying

### SPARQL

### Indexes

### Custom insights