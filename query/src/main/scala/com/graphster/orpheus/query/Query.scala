package com.graphster.orpheus.query

import com.graphster.orpheus.query.config.QueryConfig
import com.gsk.kg.engine.Compiler
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.{StringType, StructType}

object Query {
  object implicits {
    implicit def queryable(dataFrame: DataFrame): QueryableDataFrame = new QueryableDataFrame(dataFrame)
    class QueryableDataFrame(dataFrame: DataFrame) {
      def sparql(config: QueryConfig, query: String): DataFrame = {
        require {
          dataFrame.schema.fields.exists(f => f.name == "s" && f.dataType == StringType) &&
            dataFrame.schema.fields.exists(f => f.name == "p" && f.dataType == StringType) &&
            dataFrame.schema.fields.exists(f => f.name == "o" && f.dataType == StringType)
        }
        val fullQuery = config.prefixes.toSparql + query
        Compiler.compile(dataFrame, None, fullQuery, config.bellmanConfig)(dataFrame.sqlContext) match {
          case Right(result) => result
          case Left(error) =>
            val msg = s"$fullQuery\n${error.toString}"
            throw new QueryError(msg)
        }
      }

      def convertResults(cast: Map[String, String]): DataFrame = {
        dataFrame.selectExpr(
          dataFrame.schema.fields.map {
            case field if cast.contains(field.name) =>
              s"CAST(${field.name}.value AS ${cast(field.name)}) AS ${field.name}"
            case field => s"${field.name}.value AS ${field.name}"
          }: _*)
      }
    }
  }
}

class QueryError(msg: String = null, throwable: Throwable = null) extends Exception(msg, throwable)