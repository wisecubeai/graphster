package com.wisecube.orpheus.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.wisecube.orpheus.config.types.{MetadataField, MetadataFieldType}
import org.apache.spark.sql.types.Metadata
import spire.ClassTag

trait Conf extends Serializable {
  def metadata: Metadata

  def json: String = metadata.json

  def yaml: String = {
    import com.fasterxml.jackson.databind.json.JsonMapper
    import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
    val jm = new ObjectMapper()
    val ym = new ObjectMapper(new YAMLFactory)
    ym.writeValueAsString(jm.readTree(json))
  }

  def keys: Set[String]

  def keyTypes: Map[String, MetadataFieldType]

  def contains(key: String): Boolean = keys.contains(key)

  def get(key: String): MetadataField[_]

  def getOrElse(key: String): Option[MetadataField[_]] =
    if (contains(key)) {
      Some(get(key))
    } else {
      None
    }

  def getType(key: String): MetadataFieldType = get(key).fieldType
}

abstract class ConfBuilder[T <: Conf : ClassTag] {
  def fromMetadata(metadata: Metadata): T
}

object ConfBuilder {
  val ConfTypeKey: String = "type"
}