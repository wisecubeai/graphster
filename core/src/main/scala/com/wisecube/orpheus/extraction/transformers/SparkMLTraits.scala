package com.wisecube.orpheus.extraction.transformers

import com.wisecube.orpheus.graph.ValueMeta
import org.apache.spark.ml.param.shared.{HasInputCol, HasInputCols, HasOutputCol, HasOutputCols}
import org.apache.spark.ml.param.{Param, Params}

trait HasInputColwSet extends HasInputCol {
  def setInputCol(value: String): this.type = set(inputCol, value)
}

trait HasInputColswSet extends HasInputCols {
  def setInputCols(value: Array[String]): this.type = set(inputCols, value)
}

trait HasOutputColwSet extends HasOutputCol {
  def setOutputCol(value: String): this.type = set(outputCol, value)
}

trait HasOutputColswSet extends HasOutputCols {
  def setOutputCols(value: Array[String]): this.type = set(outputCols, value)
}

trait RefTable extends Params {

  final val referenceTable: Param[String] = new Param[String](this, "referenceTable", "reference table with a graph schema with entity IDs")

  final def getReferenceTable: String = $(referenceTable)

  def setReferenceTable(value: String): this.type = this.set(referenceTable, value)
}

trait HasValue extends Params {
  final val valueMeta: Param[ValueMeta] = new Param[ValueMeta](this, "valueMeta", "the metadata for the value to be built")
  final def getValueMeta: ValueMeta = $(valueMeta)
  def setValueMeta(value: ValueMeta): this.type = this.set(valueMeta, value)
}

trait HasValues extends Params {
  final val valueMetas: Param[Array[ValueMeta]] = new Param[Array[ValueMeta]](this, "valueMetas", "the array of metadata for the values to be built")
  final def getValueMetas: Array[ValueMeta] = $(valueMetas)
  def setValueMetas(value: Array[ValueMeta]): this.type = this.set(valueMetas, value)
}
