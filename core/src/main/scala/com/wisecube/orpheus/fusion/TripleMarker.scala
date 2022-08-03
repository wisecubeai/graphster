package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.HasInputColwSet
import com.wisecube.orpheus.SparkImplicits._
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset}

class TripleMarker(override val uid: String) extends Transformer with HasInputColwSet with HasTriple {

  def this() = this(Identifiable.randomUID("IdMatcher"))

  override def transform(dataset: Dataset[_]): DataFrame = {
    dataset.select(dataset.schema.fields.map {
      case f if f.name == $(inputCol) => dataset(f.name).as($(inputCol), f.metadata.addMetadata("triples", $(tripleMeta).metadata))
      case f => dataset(f.name)
    }: _*)
  }

  override def copy(extra: ParamMap): TripleMarker = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    StructType(schema.fields.map {
      case f if f.name == $(inputCol) => f.copy(name = $(inputCol), metadata = f.metadata.addMetadata("triples", $(tripleMeta).metadata))
      case f => f
    })
  }

}
