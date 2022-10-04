package com.graphster.orpheus.fusion

import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.graph.{NodeConf, TripleGraphConf}
import com.graphster.orpheus.config.Configuration
import com.graphster.orpheus.config.graph.{NodeConf, TripleGraphConf}
import org.apache.jena.graph.Node
import org.apache.spark.ml.param.{Param, Params}

trait HasTriple extends Params {
  final val tripleMeta: Param[TripleGraphConf] = new Param[TripleGraphConf](this, "tripleMeta", "the metadata for the triple")
  final def getTripleMeta: TripleGraphConf = $(tripleMeta)
  def setTripleMeta(value: TripleGraphConf): this.type = this.set(tripleMeta, value)
  def setTripleMeta(subject: Configuration, predicate: Configuration, `object`: Configuration): this.type =
    setTripleMeta(new TripleGraphConf(NodeConf(subject), NodeConf(predicate), NodeConf(`object`)))
}

trait HasTriples extends Params {
  final val triplesMeta: Param[Seq[TripleGraphConf]] = new Param[Seq[TripleGraphConf]](this, "triplesMeta", "the metadata for the triples")
  final def getTriplesMeta: Seq[TripleGraphConf] = $(triplesMeta)
  def setTriplesMeta(value: Seq[TripleGraphConf]): this.type = this.set(triplesMeta, value)
  def addTripleMeta(value: TripleGraphConf): this.type = {
    if (this.isDefined(triplesMeta)) {
      this.set(triplesMeta, $(triplesMeta) :+ value)
    } else {
      this.set(triplesMeta, Seq(value))
    }
  }
  def addTripleMeta(subject: Configuration, predicate: Configuration, `object`: Configuration): this.type =
    addTripleMeta(new TripleGraphConf(NodeConf(subject), NodeConf(predicate), NodeConf(`object`)))
}

trait HasNode extends Params {
  final val nodeMeta: Param[NodeConf] = new Param[NodeConf](this, "nodeMeta", "the metadata for the node")
  final def getNodeMeta: NodeConf = $(nodeMeta)
  def setNodeMeta(value: NodeConf): this.type = this.set(nodeMeta, value)
}
