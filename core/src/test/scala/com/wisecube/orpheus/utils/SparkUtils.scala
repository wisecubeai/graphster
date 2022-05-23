package com.wisecube.orpheus.utils

import scala.reflect.runtime.universe.TypeTag
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.{Column, DataFrame, Row, SparkSession, functions => sf}

import scala.:+

object SparkUtils {
  lazy val spark: SparkSession = SparkSession
    .builder()
    .appName("spark-test")
    .master("local[1]")
    .getOrCreate()

  def preloadTable(name: String): Unit = spark.read.option("header", "true").csv(s"./src/test/resources/$name.csv")
    .createOrReplaceTempView(name)

  def createDataFrame[A <: Product: TypeTag](rows: Seq[A], schemaOpt: Option[StructType] = None): DataFrame = {
    require(rows.nonEmpty)
    schemaOpt match {
      case Some(schema) =>
        val rdd: RDD[Row] = spark.sparkContext.parallelize(rows.map(Row.fromTuple))
        spark.createDataFrame(rdd, schema)
      case None => spark.createDataFrame(rows)
    }
  }

  def generateDataFrame(n: Int)(columns: Column*): DataFrame =
    spark.createDataFrame((0 until n).map(Tuple1.apply))
      .withColumnRenamed("_1", "ix")
      .select(sf.col("ix") +: columns.toArray: _*)
}
