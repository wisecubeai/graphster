package com.graphster.orpheus.text.fusion

import com.graphster.orpheus.SparkImplicits.MetadataWrapper
import com.graphster.orpheus.{HasInputColwSet, HasOutputColwSet}
import com.graphster.orpheus.config.graph.NodeConf
import com.graphster.orpheus.fusion.{HasTriple, HasTriples}
import com.graphster.orpheus.text.config.annotation.{NamedEntityConf, NamedEntityRow}
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset}

class NamedEntityReference(override val uid: String) extends Transformer with HasOutputColwSet with HasTriples {

  def this() = this(Identifiable.randomUID("NamedEntityReference"))

  override def copy(extra: ParamMap): Transformer = defaultCopy(extra)

  final val namedEntityConf: Param[NamedEntityConf] = new Param[NamedEntityConf](this,
    "namedEntityConf", "the metadata for the NER in the dataset")
  final def getNamedEntityConf: NamedEntityConf = $(namedEntityConf)
  def setNamedEntityConf(value: NamedEntityConf): this.type = this.set(namedEntityConf, value)

  override def transform(dataset: Dataset[_]): DataFrame = {
    val entityCol = $(namedEntityConf).toColumn
    val meta = $(triplesMeta).foldLeft($(namedEntityConf).metadata) {
      case (m, tm) => m.metadata.addMetadata(tm.name, tm.metadata)
    }
    val entityColWTriples = entityCol.as($(outputCol), meta)
    dataset.withColumn($(outputCol), entityColWTriples)
  }

  override def transformSchema(schema: StructType): StructType = {
    val meta = $(triplesMeta).foldLeft($(namedEntityConf).metadata) {
      case (m, tm) => m.metadata.addMetadata(tm.name, tm.metadata)
    }
    schema.add(StructField($(outputCol), NamedEntityRow.schema, metadata = meta))
  }
}
