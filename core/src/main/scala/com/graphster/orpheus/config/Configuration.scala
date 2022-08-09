package com.graphster.orpheus.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.graphster.orpheus.config
import com.graphster.orpheus.config.types._
import org.apache.spark.sql.types.{Metadata, MetadataBuilder}

import java.io.File
import scala.collection.JavaConverters._

trait AbstractConfiguration extends Conf {
  def getPath(key: String): ConfPath = config.ConfPath(AbstractConfiguration.RootName + key, get(key))

  def /(key: String): ConfPath = getPath(key)
}

object AbstractConfiguration {
  val RootName: String = "/"
}

case class Configuration(fields: Map[String, MetadataField[_]] = Map.empty)
  extends AbstractConfiguration
    with LongFieldsConfiguration
    with DoubleFieldsConfiguration
    with BooleanFieldsConfiguration
    with StringFieldsConfiguration
    with ConfFieldsConfiguration {
  override val metadata: Metadata = {
    val builder = new MetadataBuilder
    fields.foreach {
      case (key, LongField(value)) => builder.putLong(key, value)
      case (key, DoubleField(value)) => builder.putDouble(key, value)
      case (key, BooleanField(value)) => builder.putBoolean(key, value)
      case (key, StringField(value)) => builder.putString(key, value)
      case (key, ConfField(value)) => builder.putMetadata(key, value.metadata)
      case (key, LongSeqField(values)) => builder.putLongArray(key, values.toArray)
      case (key, DoubleSeqField(values)) => builder.putDoubleArray(key, values.toArray)
      case (key, BooleanSeqField(values)) => builder.putBooleanArray(key, values.toArray)
      case (key, StringSeqField(values)) => builder.putStringArray(key, values.toArray)
      case (key, ConfSeqField(values)) => builder.putMetadataArray(key, values.map(_.metadata).toArray)
    }
    builder.build()
  }

  override val keys: Set[String] = fields.keySet

  override def keyTypes: Map[String, MetadataFieldType] = fields.mapValues(_.fieldType)

  override def get(key: String): MetadataField[_] = fields(key)
}

object Configuration extends ConfBuilder[Configuration] {
  val empty: Configuration = Configuration()

  def apply(conf: Conf): Configuration = conf match {
    case c: Configuration => c
    case _ => {
      val fields: Map[String, MetadataField[_]] = conf.keys.map(k => (k, conf.get(k))).map {
        case (k: String, f: LongField) => (k, f)
        case (k: String, f: DoubleField) => (k, f)
        case (k: String, f: BooleanField) => (k, f)
        case (k: String, f: StringField) => (k, f)
        case (k: String, f: ConfField) => (k, f)
        case (k: String, f: LongSeqField) => (k, f)
        case (k: String, f: DoubleSeqField) => (k, f)
        case (k: String, f: BooleanSeqField) => (k, f)
        case (k: String, f: StringSeqField) => (k, f)
        case (k: String, f: ConfSeqField) => (k, f)
      }.toMap
      Configuration(fields)
    }
  }

  private def parseTree(tree: JsonNode): Configuration = {
    val fields = tree.fields().asScala.map(e => (e.getKey, e.getValue)).map {
      case (key, node) if node.isIntegralNumber => (key, MetadataField(node.asLong()))
      case (key, node) if node.isFloatingPointNumber => (key, MetadataField(node.asDouble()))
      case (key, node) if node.isBoolean => (key, MetadataField(node.asBoolean()))
      case (key, node) if node.isTextual => (key, MetadataField(node.asText()))
      case (key, node) if node.isObject => (key, MetadataField(parseTree(node)))
      case (key, nodeArr) if nodeArr.isArray =>
        val arr = nodeArr.elements().asScala.toArray
        val seqField = arr.head match {
          case node if node.isIntegralNumber => LongSeqField(arr.map(parseLong))
          case node if node.isFloatingPointNumber => DoubleSeqField(arr.map(parseDouble))
          case node if node.isBoolean => BooleanSeqField(arr.map(parseBoolean))
          case node if node.isTextual => StringSeqField(arr.map(parseString))
          case node if node.isObject => ConfSeqField(arr.map(parseTree))
        }
        (key, seqField)
    }.toMap
    new Configuration(fields)
  }

  private def parseLong(longNode: JsonNode): Long = longNode.asLong()

  private def parseDouble(doubleNode: JsonNode): Double = doubleNode.asDouble()

  private def parseBoolean(boolNode: JsonNode): Boolean = boolNode.asBoolean()

  private def parseString(textNode: JsonNode): String = textNode.asText()

  def apply(fields: (String, MetadataField[_])*): Configuration = Configuration(fields.toMap)

  override def fromMetadata(metadata: Metadata): Configuration = loadJSONString(metadata.json)

  def loadJSONString(json: String): Configuration = {
    val jsonMapper = new JsonMapper()
    val tree = jsonMapper.readTree(json)
    parseTree(tree)
  }

  def loadJSON(file: File): Configuration = {
    val src = scala.io.Source.fromFile(file)
    val json = src.mkString
    src.close()
    loadJSONString(json)
  }

  def loadYAMLString(yaml: String): Configuration = {
    val yamlMapper = new YAMLMapper()
    val tree = yamlMapper.readTree(yaml)
    parseTree(tree)
  }

  def loadYAML(file: File): Configuration = {
    val src = scala.io.Source.fromFile(file)
    val yaml = src.mkString
    src.close()
    loadYAMLString(yaml)
  }

}

trait LongFieldsConfiguration {
  self: AbstractConfiguration =>
  def getLong(key: String): Long = metadata.getLong(key)
  def getLongSeq(key: String): Array[Long] = metadata.getLongArray(key)
  def hasLong(key: String): Boolean = contains(key) && keyTypes(key) == LongFieldType
  def hasLongSeq(key: String): Boolean = contains(key) && keyTypes(key) == LongSeqFieldType
}

trait DoubleFieldsConfiguration {
  self: AbstractConfiguration =>
  def getDouble(key: String): Double = metadata.getDouble(key)
  def getDoubleSeq(key: String): Array[Double] = metadata.getDoubleArray(key)
  def hasDouble(key: String): Boolean = contains(key) && keyTypes(key) == DoubleFieldType
  def hasDoubleSeq(key: String): Boolean = contains(key) && keyTypes(key) == DoubleSeqFieldType
}

trait BooleanFieldsConfiguration {
  self: AbstractConfiguration =>
  def getBoolean(key: String): Boolean = metadata.getBoolean(key)
  def getBooleanSeq(key: String): Array[Boolean] = metadata.getBooleanArray(key)
  def hasBoolean(key: String): Boolean = contains(key) && keyTypes(key) == BooleanFieldType
  def hasBooleanSeq(key: String): Boolean = contains(key) && keyTypes(key) == BooleanSeqFieldType
}

trait StringFieldsConfiguration {
  self: AbstractConfiguration =>
  def getString(key: String): String = metadata.getString(key)
  def getStringSeq(key: String): Array[String] = metadata.getStringArray(key)
  def hasString(key: String): Boolean = contains(key) && keyTypes(key) == StringFieldType
  def hasStringSeq(key: String): Boolean = contains(key) && keyTypes(key) == StringSeqFieldType
}

trait ConfFieldsConfiguration {
  self: AbstractConfiguration =>
  def getConf(key: String): Configuration = {
    Configuration.fromMetadata(metadata.getMetadata(key))
  }
  def getConfSeq(key: String): Array[Configuration] = {
    val metadataArr = metadata.getMetadataArray(key)
    metadataArr.map(Configuration.fromMetadata)
  }

  def hasConf(key: String): Boolean = contains(key) && keyTypes(key) == ConfFieldType
  def hasConfSeq(key: String): Boolean = contains(key) && keyTypes(key) == ConfSeqFieldType
}