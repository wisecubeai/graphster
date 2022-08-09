package com.graphster.orpheus

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.graphster.orpheus.config.types._
import org.apache.spark.sql.types.Metadata

import scala.util.Try
import scala.collection.JavaConverters._

object Utils {
  def trycall[R](function: () => R)(default: PartialFunction[Throwable, R]): R =
    Try(function()).recover(default).get
  def trycall[R, A](function: A => R)(default: PartialFunction[Throwable, R])(arg: A): R =
    Try(function(arg)).recover(default).get
  def trycall[R, A1, A2](function: (A1, A2) => R)(default: PartialFunction[Throwable, R])(arg1: A1, arg2: A2): R =
    Try(function(arg1, arg2)).recover(default).get
  def trycall[R, A1, A2, A3](function: (A1, A2, A3) => R)(default: PartialFunction[Throwable, R])(arg1: A1, arg2: A2, arg3: A3): R =
    Try(function(arg1, arg2, arg3)).recover(default).get
  def trycall[R, A1, A2, A3, A4](function: (A1, A2, A3, A4) => R)(default: PartialFunction[Throwable, R])(arg1: A1, arg2: A2, arg3: A3, arg4: A4): R =
    Try(function(arg1, arg2, arg3, arg4)).recover(default).get

  def metadataKeyTypes(metadata: Metadata): Map[String, MetadataFieldType] = {
    val mapper = new ObjectMapper()
    val json = metadata.json
    val tree = mapper.readTree(json)
    tree.fields().asScala.map(e => (e.getKey, e.getValue)).map {
      case (key, value) => value.getNodeType match {
        case JsonNodeType.NUMBER if value.isIntegralNumber => (key, LongFieldType)
        case JsonNodeType.NUMBER if value.isFloatingPointNumber => (key, DoubleFieldType)
        case JsonNodeType.BOOLEAN => (key, BooleanFieldType)
        case JsonNodeType.STRING => (key, StringFieldType)
        case JsonNodeType.OBJECT => (key, ConfFieldType)
        case JsonNodeType.ARRAY =>
          val first = value.elements().asScala.next()
          first.getNodeType match {
            case JsonNodeType.NUMBER if value.isIntegralNumber => (key, LongSeqFieldType)
            case JsonNodeType.NUMBER if value.isFloatingPointNumber => (key, DoubleSeqFieldType)
            case JsonNodeType.BOOLEAN => (key, BooleanSeqFieldType)
            case JsonNodeType.STRING => (key, StringSeqFieldType)
            case JsonNodeType.OBJECT => (key, ConfSeqFieldType)
          }
      }
    }.toMap
  }
}
