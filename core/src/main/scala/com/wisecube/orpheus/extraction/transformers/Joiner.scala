package com.wisecube.orpheus.extraction.transformers

import com.wisecube.orpheus.graph.ValueMeta
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

/**
 * This class is used to match IDs from a reference table to entities in a table
 * @param uid
 */
class Joiner(override val uid: String) extends Transformer with RefTable with HasValues with HasOutputColswSet {

  def this() = this(Identifiable.randomUID("Joiner"))

  /**
   * This parameter is a flag that indicates the join type
   * @see org.apache.spark.sql.Dataset#joinWith
   */
  final val joinType: Param[String] = new Param[String](this, "joinType", "the name of the join type to use")

  /**
   * Get flag that indicates the join type
   *
   * @return the flag that indicates the join type
   */
  final def getJoinType: String = $(joinType)

  def setJoinType(value: String): this.type = this.set(joinType, value)

  override def setValueMetas(value: Array[ValueMeta]): this.type = {
    require(value.length % 2 == 0, "There must be an even number of join columns (left1, right1, left2, right2, ...)")
    super.setValueMetas(value)
  }

  override def transform(dataset: Dataset[_]): DataFrame = {
    val ref = dataset.sparkSession.table($(referenceTable))
    val joinCol = $(valueMetas).map(_.toColumn).grouped(2).map { case Array(left, right) => left === right }.reduce(_ && _)
    dataset.join(ref, joinCol, $(joinType)).select(
      dataset.columns.map(dataset(_)) ++ $(outputCols).map(ref(_)): _*
    )
  }

  override def copy(extra: ParamMap): Joiner = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    require($(outputCols).forall(c => !schema.fieldNames.contains(c)))
    val structFields = SparkSession.getActiveSession match {
      case Some(spark) =>
        val refSchema = spark.table($(referenceTable)).schema
        $(outputCols).map(refSchema.fieldIndex).map(refSchema.fields(_))
      case None => $(outputCols).map(StructField(_, StringType))
    }
    structFields.foldLeft(schema)(_ add _)
  }
}
