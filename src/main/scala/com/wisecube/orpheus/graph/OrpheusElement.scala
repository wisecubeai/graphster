package com.wisecube.orpheus.graph

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._

trait OrpheusElement[T] extends ValueMetaBuilder[ValueMeta] {
  val schema: StructType
  type Meta <: ValueMeta

  def jena2row(elem: T): Row
  def row2jena(row: Row): T
  def jena2string(elem: T): String
  def string2jena(str: String): T
  override def fromMetadata(metadata: Metadata): Meta
}













