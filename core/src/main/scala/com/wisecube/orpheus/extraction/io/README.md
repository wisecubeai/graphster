Example 3. loading RDF data into a dataframe
```scala
import org.apache.spark.sql.{DataFrame, SparkSession}

val spark: SparkSession = SparkSession.builder.getOrCreate()

val rdfData: DataFrame = spark.read.format("...").load("...")
rdfData.write.saveAsTable("...")
```