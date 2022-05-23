package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.SparkImplicits._
import com.wisecube.orpheus.graph._
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset, functions => sf}

class TripleExtractor(override val uid: String) extends Transformer {

  def this() = this(Identifiable.randomUID("TripleExtractor"))

  override def transform(dataset: Dataset[_]): DataFrame = {
    val tripleMetas = dataset.schema.fields
      .flatMap(f => f.metadata.getMetadataArrayOrElse("triples"))
      .map(TripleElement.fromMetadata)
    require(tripleMetas.nonEmpty)
    val tripleCols = tripleMetas.map(_.toColumn)
    dataset
      .select(sf.array(tripleCols: _*).as("triples"))
      .select(sf.explode(sf.col("triples")).as("triple"))
      .select(
        sf.col("triple").getField(TripleElement.subjectKey).as(TripleElement.subjectKey),
        sf.col("triple").getField(TripleElement.predicateKey).as(TripleElement.predicateKey),
        sf.col("triple").getField(TripleElement.objectKey).as(TripleElement.objectKey),
      )
  }

  override def copy(extra: ParamMap): TripleExtractor = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = TripleElement.schema
}
