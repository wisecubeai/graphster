package com.wisecube.orpheus

import com.wisecube.orpheus.graph.{NodeMeta, TripleElement, ValueMeta}
import org.apache.spark.ml.param.shared.{HasInputCol, HasInputCols, HasOutputCol, HasOutputCols}
import org.apache.spark.ml.param.{Param, Params}
import org.apache.spark.sql.types.{Metadata, MetadataBuilder}

package object extractors {
  val TriplesMetadataKey = "triples"
  val NodesKey = "nodes"

  trait HasInputColwSet extends HasInputCol {
    def setInputCol(value: String): this.type = set(inputCol, value)
  }

  trait HasInputColswSet extends HasInputCols {
    def setInputCols(value: Array[String]): this.type = set(inputCols, value)
  }

  trait HasOutputColwSet extends HasOutputCol {
    def setOutputCol(value: String): this.type = set(outputCol, value)
  }

  trait HasOutputColswSet extends HasOutputCols {
    def setOutputCols(value: Array[String]): this.type = set(outputCols, value)
  }

  trait RefTable extends Params {

    final val referenceTable: Param[String] = new Param[String](this, "referenceTable", "reference table with a graph schema with entity IDs")

    final def getReferenceTable: String = $(referenceTable)

    def setReferenceTable(value: String): this.type = this.set(referenceTable, value)
  }

  trait HasTriple extends Params {
    final val tripleMeta: Param[TripleElement.Meta] = new Param[TripleElement.Meta](this, "tripleMeta", "the metadata for the triple")
    final def getTripleMeta: TripleElement.Meta = $(tripleMeta)
    def setTripleMeta(value: TripleElement.Meta): this.type = this.set(tripleMeta, value)
  }

  trait HasNode extends Params {
    final val nodeMeta: Param[NodeMeta] = new Param[NodeMeta](this, "nodeMeta", "the metadata for the node")
    final def getNodeMeta: NodeMeta = $(nodeMeta)
    def setNodeMeta(value: NodeMeta): this.type = this.set(nodeMeta, value)
  }

  trait HasValue extends Params {
    final val valueMeta: Param[ValueMeta] = new Param[ValueMeta](this, "valueMeta", "the metadata for the value to be built")
    final def getValueMeta: ValueMeta = $(valueMeta)
    def setValueMeta(value: ValueMeta): this.type = this.set(valueMeta, value)
  }

  trait HasValues extends Params {
    final val valueMetas: Param[Array[ValueMeta]] = new Param[Array[ValueMeta]](this, "valueMetas", "the array of metadata for the values to be built")
    final def getValueMetas: Array[ValueMeta] = $(valueMetas)
    def setValueMetas(value: Array[ValueMeta]): this.type = this.set(valueMetas, value)
  }

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
