package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.graph.{NodeMeta, TripleElement}
import org.apache.spark.ml.param.{Param, Params}

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
