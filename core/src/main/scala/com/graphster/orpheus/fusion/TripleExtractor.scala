package com.graphster.orpheus.fusion

import com.graphster.orpheus.config.graph.TripleGraphConf
import com.graphster.orpheus.SparkImplicits._
import com.graphster.orpheus.config._
import com.graphster.orpheus.config.graph.TripleGraphConf
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
      .map(TripleGraphConf.fromMetadata)
    require(tripleMetas.nonEmpty)
    val tripleCols = tripleMetas.map(_.toColumn)
    dataset
      .select(sf.array(tripleCols: _*).as("triples"))
      .select(sf.explode(sf.col("triples")).as("triple"))
      .select(
        sf.col("triple").getField(TripleGraphConf.SubjectKey).as(TripleGraphConf.SubjectKey),
        sf.col("triple").getField(TripleGraphConf.PredicateKey).as(TripleGraphConf.PredicateKey),
        sf.col("triple").getField(TripleGraphConf.ObjectKey).as(TripleGraphConf.ObjectKey),
      )
  }

  override def copy(extra: ParamMap): TripleExtractor = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = TripleGraphConf.schema
}
