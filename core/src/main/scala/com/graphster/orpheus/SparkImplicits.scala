package com.graphster.orpheus

import org.apache.spark.sql.types.{Metadata, MetadataBuilder}

object SparkImplicits {
  implicit class MetadataWrapper(val metadata: Metadata) {
    def getLongOrElse(key: String, default: Option[Long] = None): Long = if (metadata.contains(key)) {
      metadata.getLong(key)
    } else {
      default.getOrElse(0L)
    }

    def getDoubleOrElse(key: String, default: Option[Double] = None): Double = if (metadata.contains(key)) {
      metadata.getDouble(key)
    } else {
      default.getOrElse(0.0)
    }

    def getBooleanOrElse(key: String, default: Option[Boolean] = None): Boolean = if (metadata.contains(key)) {
      metadata.getBoolean(key)
    } else {
      default.getOrElse(false)
    }

    def getStringOrElse(key: String, default: Option[String] = None): String = if (metadata.contains(key)) {
      metadata.getString(key)
    } else {
      default.getOrElse("")
    }

    def getMetadataOrElse(key: String, default: Option[Metadata] = None): Metadata = if (metadata.contains(key)) {
      metadata.getMetadata(key)
    } else {
      default.getOrElse(Metadata.empty)
    }

    def getLongArrayOrElse(key: String, default: Option[Array[Long]] = None): Array[Long] = if (metadata.contains(key)) {
      metadata.getLongArray(key)
    } else {
      default.getOrElse(Array.empty)
    }

    def getDoubleArrayOrElse(key: String, default: Option[Array[Double]] = None): Array[Double] = if (metadata.contains(key)) {
      metadata.getDoubleArray(key)
    } else {
      default.getOrElse(Array.empty)
    }

    def getBooleanArrayOrElse(key: String, default: Option[Array[Boolean]] = None): Array[Boolean] = if (metadata.contains(key)) {
      metadata.getBooleanArray(key)
    } else {
      default.getOrElse(Array.empty)
    }

    def getStringArrayOrElse(key: String, default: Option[Array[String]] = None): Array[String] = if (metadata.contains(key)) {
      metadata.getStringArray(key)
    } else {
      default.getOrElse(Array.empty)
    }

    def getMetadataArrayOrElse(key: String, default: Option[Array[Metadata]] = None): Array[Metadata] = if (metadata.contains(key)) {
      metadata.getMetadataArray(key)
    } else {
      default.getOrElse(Array.empty)
    }

    def addLong(key: String, value: Long): Metadata = {
      new MetadataBuilder().withMetadata(metadata).putLongArray(key, getLongArrayOrElse(key) :+ value).build()
    }

    def addDouble(key: String, value: Double): Metadata = {
      new MetadataBuilder().withMetadata(metadata).putDoubleArray(key, getDoubleArrayOrElse(key) :+ value).build()
    }

    def addBoolean(key: String, value: Boolean): Metadata = {
      new MetadataBuilder().withMetadata(metadata).putBooleanArray(key, getBooleanArrayOrElse(key) :+ value).build()
    }

    def addString(key: String, value: String): Metadata = {
      new MetadataBuilder().withMetadata(metadata).putStringArray(key, getStringArrayOrElse(key) :+ value).build()
    }

    def addMetadata(key: String, value: Metadata): Metadata = {
      new MetadataBuilder().withMetadata(metadata).putMetadataArray(key, getMetadataArrayOrElse(key) :+ value).build()
    }
  }
}
