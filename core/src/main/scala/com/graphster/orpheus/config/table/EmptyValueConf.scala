package com.graphster.orpheus.config.table

import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf, ValueConfBuilder, types}
import org.apache.spark.sql.{Column, functions => sf}

case class EmptyValueConf private() extends ValueConf(Configuration.empty) with AtomicValue {
  override val name: String = EmptyValueConf.Name

  override def toColumn: Column = sf.lit(null).cast("string").as(name)

  override def keys: Set[String] = Set.empty

  override def keyTypes: Map[String, types.MetadataFieldType] = Map.empty

  override def get(key: String): types.MetadataField[_] =
    throw new NoSuchFieldException(s"${EmptyValueConf.Name} has no fields")
}

object EmptyValueConf extends ValueConfBuilder[EmptyValueConf] {
  private val instance = new EmptyValueConf()
  val Name: String = "_EMPTY_"

  def apply(): EmptyValueConf = instance

  override def apply(config: Configuration): EmptyValueConf = instance
}