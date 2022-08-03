package com.wisecube.orpheus.fusion

import com.wisecube.orpheus.config.Configuration
import com.wisecube.orpheus.config.graph.{NodeConf, TripleGraphConf}
import org.apache.jena.graph.Node
import org.apache.spark.ml.param.{Param, Params}

trait HasTriple extends Params {
  final val tripleMeta: Param[TripleGraphConf] = new Param[TripleGraphConf](this, "tripleMeta", "the metadata for the triple")
  final def getTripleMeta: TripleGraphConf = $(tripleMeta)
  def setTripleMeta(value: TripleGraphConf): this.type = this.set(tripleMeta, value)
  def setTripleMeta(name: String, subject: Configuration, predicate: Configuration, `object`: Configuration): this.type =
    setTripleMeta(new TripleGraphConf(name, NodeConf(subject), NodeConf(predicate), NodeConf(`object`)))
}

trait HasNode extends Params {
  final val nodeMeta: Param[NodeConf] = new Param[NodeConf](this, "nodeMeta", "the metadata for the node")
  final def getNodeMeta: NodeConf = $(nodeMeta)
  def setNodeMeta(value: NodeConf): this.type = this.set(nodeMeta, value)
}
