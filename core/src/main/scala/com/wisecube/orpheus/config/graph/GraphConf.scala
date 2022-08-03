package com.wisecube.orpheus.config.graph

import com.wisecube.orpheus.config.{Configuration, ValueConf, ValueConfBuilder}
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

abstract class GraphConf[T](config: Configuration) extends ValueConf(config: Configuration)

trait GraphConfBuilder[T, U <: GraphConf[T]] extends ValueConfBuilder[U] {
  def schema: StructType

  def jena2row(elem: T): Row

  def row2jena(row: Row): T

  def jena2string(elem: T): String

  def string2jena(str: String): T
}