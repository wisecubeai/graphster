package com.wisecube.orpheus.enrichment

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{BooleanParam, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset}

class ValueBuilder(override val uid: String) extends Transformer with HasValue {

  def this() = this(Identifiable.randomUID("ValueBuilder"))

  final val isNullable: BooleanParam = new BooleanParam(this, "isNullable", "flag for whether or not this value can be null")
  final def getIsNullable: Boolean = $(isNullable)
  def setIsNullable(value: Boolean): this.type = this.set(isNullable, value)

  setDefault(isNullable, true)

  override def transform(dataset: Dataset[_]): DataFrame = dataset.select(
    dataset.columns.map(c => dataset(c)) :+ $(valueMeta).toColumn: _*
  )

  override def copy(extra: ParamMap): Transformer = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    schema.add(StructField($(valueMeta).name, StringType, $(isNullable), $(valueMeta).metadata))
  }
}
