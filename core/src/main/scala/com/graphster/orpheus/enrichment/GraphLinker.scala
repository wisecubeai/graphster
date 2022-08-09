package com.graphster.orpheus.enrichment

import com.graphster.orpheus.config.graph.NodeConf
import com.graphster.orpheus.config.graph.{NodeConf, TripleGraphConf}
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.types.{StringType, StructType}
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession, functions => sf}

/**
 * This class is used to match IDs from a reference table to entities in a table
 * @param uid
 */
class GraphLinker(override val uid: String) extends Transformer with RefTable with HasOutputColwSet {

  def this() = this(Identifiable.randomUID("PropertyEnricher"))

  /**
   * This paramater represents the column in the target table that contains the property to join on
   */
  final val propertyCol: Param[String] = new Param[String](this, "propertyCol",
    "the column in the target table that contains the property to join on")

  def getPropertyCol: String = $(propertyCol)

  def setPropertyCol(value: String): this.type = this.set(propertyCol, value)

  final val predicate: Param[String] = new Param[String](this, "predicate",
    "the predicate URI for the property")

  def getPredicateCol: String = $(predicate)

  def setPredicateCol(value: String): this.type = this.set(predicate, value)

  final val transformation: Param[String] = new Param[String](this, "transformation",
  "the transformation expression to use on both sides othe join condition, e.g. LOWER($), the $ will be replaced")

  def getTransformation: String = $(transformation)

  def setTransformation(value: String): this.type = this.set(transformation, value)

  /**
   * This parameter indicates the join type
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

  setDefault(joinType, "left-outer")

  override def transform(dataset: Dataset[_]): DataFrame = {
    val ref = dataset.sparkSession.table($(referenceTable))
    val filteredRef = ref.filter(ref(TripleGraphConf.PredicateKey).getField(NodeConf.UriKey) === $(predicate))
    val joinCol = if (this.isSet(transformation)) {
      sf.expr($(transformation).replace("$", $(propertyCol))) ===
        sf.expr($(transformation).replace("$", "object.lex"))
    } else {
      sf.col($(propertyCol)) === sf.expr("object.lex")
    }
    dataset.join(filteredRef, joinCol, $(joinType)).select(
      dataset.columns.map(dataset(_)) :+ filteredRef(TripleGraphConf.SubjectKey).getField(NodeConf.UriKey).as($(outputCol)): _*
    )
  }

  override def copy(extra: ParamMap): GraphLinker = defaultCopy(extra)

  override def transformSchema(schema: StructType): StructType = {
    require(!schema.fieldNames.contains($(outputCol)))
    val spark = SparkSession.builder().getOrCreate()
    require(spark.table($(referenceTable)).schema.simpleString == TripleGraphConf.schema.simpleString)
    schema.add($(outputCol), StringType)
  }
}
