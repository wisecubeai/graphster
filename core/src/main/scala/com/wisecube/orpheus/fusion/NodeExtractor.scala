package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.graph.{NodeElement, ValueMeta}
import com.wisecube.orpheus.SparkImplicits._
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset}

class NodeExtractor(override val uid: String) extends Transformer {

  def this() = this(Identifiable.randomUID("NodeExtractor"))

  override def transform(dataset: Dataset[_]): DataFrame = dataset.select(
    dataset.columns.map(c => dataset(c)) ++
      dataset.schema.fields
        .filter(f => f.metadata.contains(NodesKey))
        .flatMap(_.metadata.getMetadataArrayOrElse(NodesKey))
        .map(NodeElement.fromMetadata).map(_.toColumn): _*
  )

  override def copy(extra: ParamMap): Transformer = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    schema.fields
      .filter(f => f.metadata.contains(NodesKey))
      .flatMap(_.metadata.getMetadataArrayOrElse(NodesKey))
      .map(m => StructField(m.getString(ValueMeta.nameKey), StringType, nullable = true, metadata = m))
      .foldLeft(schema)(_ add _)
  }
}
