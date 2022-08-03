package com.wisecube.orpheus.enrichment

import com.wisecube.orpheus.config.table.ConcatValueConf
import com.wisecube.orpheus.config.{Configuration, ValueConf}
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
  final val valueMeta: Param[ValueConf] = new Param[ValueConf](this, "valueMeta", "the metadata for the value to be built")
  final def getValueMeta: ValueConf = $(valueMeta)
  def setValueMeta(value: ValueConf): this.type = this.set(valueMeta, value)
  def setValueMeta(value: Configuration): this.type = setValueMeta(ValueConf.fromConfiguration(value))
}

trait HasValues extends Params {
  final val valueMetas: Param[Array[ValueConf]] = new Param[Array[ValueConf]](this, "valueMetas", "the array of metadata for the values to be built")
  final def getValueMetas: Array[ValueConf] = $(valueMetas)
  def setValueMetas(value: Array[ValueConf]): this.type = this.set(valueMetas, value)
}
