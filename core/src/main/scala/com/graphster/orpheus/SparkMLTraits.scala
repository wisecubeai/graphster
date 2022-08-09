package com.graphster.orpheus

import org.apache.spark.ml.param.shared.{HasInputCol, HasInputCols, HasOutputCol, HasOutputCols}

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
