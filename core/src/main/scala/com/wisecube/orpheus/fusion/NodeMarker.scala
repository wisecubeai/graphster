package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.HasInputColwSet
import com.wisecube.orpheus.SparkImplicits._
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{DataFrame, Dataset}

class NodeMarker(override val uid: String) extends Transformer with HasInputColwSet with HasNode {

  def this() = this(Identifiable.randomUID("NodeMarker"))

  override def transform(dataset: Dataset[_]): DataFrame = dataset.select(
    dataset.schema.fields.map {
      case markedField if markedField.name == $(inputCol) =>
        dataset($(inputCol)).as($(inputCol), markedField.metadata.addMetadata(NodesKey, $(nodeMeta).metadata))
      case otherField => dataset(otherField.name)
    }: _*
  )

  override def copy(extra: ParamMap): Transformer = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = StructType(schema.fields.map {
      case markedField if markedField.name == $(inputCol) =>
        markedField.copy($(inputCol), metadata = markedField.metadata.addMetadata(NodesKey, $(nodeMeta).metadata))
      case otherField => otherField
  })
}