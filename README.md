# wisecube

`wisecube` is an open-source platform for scalable, end-to-end knowledge graph construction from unstructured text. The platform takes a collection of documents, extracts mentions and relations to populate a raw knowledge graph, links mentions to entities in Wikidata, and then enriches the knowledge graph with facts from Wikidata.
See [`wisecube.ai`](http://wisecube.ai/) for an overview of the platform.

The current `wisecube` demo "distills" the [TREC Washington Post Corpus](https://trec.nist.gov/data/wapost/) containing around 600K documents into a raw knowledge graph comprised of approximately 97M triples, enriched with facts from Wikidata for the 324K distinct entities discovered in the corpus.
On top of this knowledge graph, we have implemented a subgraph-matching approach to align extracted relations with facts from Wikidata using the declarative Cypher query language.
This simple demo shows that fact verification, locating textual support for asserted facts, detecting inconsistent and missing facts, and extracting distantly-supervised training data can all be performed within the same framework.
 
This README provies instructions on how to replicate our work.

# Setup

Clone [wisecube](https://github.com/thecloudcircle/wisecube):

```
git clone https://github.com/thecloudcircle/wisecube.git
```

[sbt](https://www.scala-sbt.org/) is the build tool used for Scala projects, download it if you don't have it yet.

Build the JAR using sbt:

```
sbt assembly
````

There is a [known issue](https://github.com/stanfordnlp/CoreNLP/issues/556) between recent Spark versions and CoreNLP 3.8. To fix this, delete the `protobuf-java-2.5.0.jar` file in `$SPARK_HOME/jars` and replace it with [version 3.0.0](https://repo1.maven.org/maven2/com/google/protobuf/protobuf-java/3.0.0/protobuf-java-3.0.0.jar).

## Anserini

Download and build [Anserini](http://anserini.io).

Follow the [Solrini](https://github.com/castorini/anserini/blob/master/docs/solrini.md) instructions to set up a SolrCloud instance and index a document collection into SolrCloud, such as the [TREC Washington Post Corpus](https://github.com/castorini/anserini/blob/master/docs/regressions-core18.md).

## neo4j

Start a neo4j instance via Docker with the command:
```bash
docker run -d --name neo4j --publish=7474:7474 --publish=7687:7687 \
    --volume=`pwd`/neo4j:/data \
    -e NEO4J_dbms_memory_pagecache_size=2G \
    -e NEO4J_dbms_memory_heap_initial__size=4G \
    -e NEO4J_dbms_memory_heap_max__size=16G \
    neo4j
```

Note: You may wish to update the memory settings based on the amount of available memory on your machine.

neo4j should should be available shortly at [http://localhost:7474/](http://localhost:7474/) with the default username/password of `neo4j`/`neo4j`. You will be prompted to change the password, this is the password you will pass to the load script.

## Running

### Extraction

For each document in the collection, we extract mentions of named entities, the relations between them, and links to entities in an external knowledge graph.

Run `ExtractTriples` using default options:

```
./bin/extract.sh
```

Note: Modify [extract.sh](./bin/extract.sh) based on your environment (e.g., available memory, number of executors, Solr, neo4j password, etc.) - options available [here](src/main/scala/io/dstlr/package.scala).

For example, the following command does the query `music` on index `core18`, then apply `dstlr` to the top 5 hits.

```
spark-submit --class io.dstlr.ExtractTriples \
        --num-executors 32 --executor-cores 8 \
        --driver-memory 64G --executor-memory 48G \
        --conf spark.executor.heartbeatInterval=10000 \
        --conf spark.executorEnv.JAVA_HOME=/usr/lib/jvm/java-9-openjdk-amd64 \
        target/scala-2.11/dstlr-assembly-0.1.jar \
        --solr.uri localhost:9983 --solr.index core18 --max_rows 5 --query contents:music --partitions 2048 --output triples --sent-length-threshold 256
```




After the extraction is done, check if an output folder (called `triples/` by default) is created, and several Parquet files are generated inside the output folder.

If you want to inspect the Parquet file:

- Download  and build [parquet-tools](https://github.com/apache/parquet-mr/tree/master/parquet-tools) following instructions.

Note: If you are on Mac, you could also install it with Homebrew `brew install parquet-tools`.

- View the Parquet file in JSON format:

```
parquet-tools cat --json [filename]
```

### Enrichment

We augment the raw knowledge graph with facts from the external knowledge graph (Wikidata in our case).

Run `EnrichTriples`:

```
./bin/enrich.sh
```

Note: Modify [enrich.sh](./bin/enrich.sh) based on your environment.

After the enrichment is done, check if an output folder (called `triples-enriched/` by default) is created with output Parquet files.

### Load

Load raw knowledge graph and enriched knowledge graph produced from the above commands to neo4j.

Set `--input triples` in `load.sh`, run `LoadTriples`:

```
./bin/load.sh
```

Note: Modify [load.sh](./bin/load.sh) based on your environment.

Set `--input triples-enriched` in `load.sh`, run `LoadTriples` again:

```
./bin/load.sh
```

Open [http://localhost:7474/](http://localhost:7474/) to view the loaded knowledge graph in neo4j.

## Data Cleaning Queries

The following queries can be run against the knowledge graph in neo4j to discover sub-graphs of interest.

### Supporting Information

This query finds sub-graphs where the value extracted from the document matches the ground-truth from Wikidata.

```
MATCH (d:Document)-->(s:Mention)-->(r:Relation)-->(o:Mention)
MATCH (s)-->(e:Entity)-->(f:Fact {relation: r.type})
WHERE o.span = f.value
RETURN d, s, r, o, e, f
```

In order to see only sub-graphs with a specific relationship such as "city of headquaters", run

```
MATCH (d:Document)-->(s:Mention)-->(r:Relation {type: "ORG_CITY_OF_HEADQUARTERS"})-->(o:Mention)
MATCH (s)-->(e:Entity)-->(f:Fact {relation: r.type})
WHERE o.span = f.value
RETURN d, s, r, o, e, f
```

### Inconsistent Information

This query finds sub-graphs where the value extracted from the document does not match the ground-truth from Wikidata.

```
MATCH (d:Document)-->(s:Mention)-->(r:Relation)-->(o:Mention)
MATCH (s)-->(e:Entity)-->(f:Fact {relation: r.type})
WHERE NOT(o.span = f.value)
RETURN d, s, r, o, e, f
```

### Missing Information

This query finds sub-graphs where the value extracted from the document does not have a corresponding ground-truth in Wikidata.

```
MATCH (d:Document)-->(s:Mention)-->(r:Relation)-->(o:Mention)
MATCH (s)-->(e:Entity)
OPTIONAL MATCH (e)-->(f:Fact {relation: r.type})
WITH d, s, r, o, e, f
WHERE f IS NULL
RETURN d, s, r, o, e, f
```

### Delete Relationships

This query deletes all relationships in the database.

```
MATCH (n) DETACH DELETE n
```
