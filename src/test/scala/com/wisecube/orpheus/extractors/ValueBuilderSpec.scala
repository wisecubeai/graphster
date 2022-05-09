package com.wisecube.orpheus.extractors

import com.wisecube.orpheus.graph.{ColumnValueMeta, ConcatValueMeta, LiteralValueMeta}
import com.wisecube.orpheus.utils.SparkUtils
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature.SQLTransformer
import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.{functions => sf}

class ValueBuilderSpec extends AnyFunSuite {
  test("testing basic processing") {
    SparkUtils.spark
    val pipeline = new Pipeline().setStages(Array(
      new SQLTransformer().setStatement("SELECT ix, ix*ix AS sq FROM __THIS__"),
      new ValueBuilder().setIsNullable(false).setValueMeta(LiteralValueMeta("protocol", "http://")),
      new ValueBuilder().setValueMeta(ConcatValueMeta("domain", Seq(LiteralValueMeta("www.example"), ColumnValueMeta("ix"), LiteralValueMeta(".com/")))),
      new ValueBuilder().setValueMeta(ConcatValueMeta("uri", Seq(ColumnValueMeta("protocol"), ColumnValueMeta("domain"), ColumnValueMeta("sq"))))
    ))
    val df = SparkUtils.generateDataFrame(10)()
    val tx = pipeline.fit(df).transform(df).withColumn("exp", sf.expr("CONCAT('http://www.example', CAST(ix AS string), '.com/', CAST(sq AS string))"))
    assert(tx.where("uri != exp").isEmpty)
  }
}
