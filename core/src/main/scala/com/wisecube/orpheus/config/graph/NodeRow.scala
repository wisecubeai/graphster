package com.wisecube.orpheus.config.graph

import org.apache.spark.sql.Row

case class NodeRow(
  `type`: String,
  uri: String = null,
  namespace: String = null,
  local: String = null,
  lex: String = null,
  language: String = null,
  datatype: String = null,
  blankId: String = null,
  error: String = null,
)

object NodeRow {
  def apply(row: Row): NodeRow = row match {
    case Row(`type`: String, uri: String, namespace: String, local: String, null, null, null, null, null) => new NodeRow(
      `type` = `type`, uri = uri, namespace = namespace, local = local
    )
    case Row(`type`: String, null, null, null, lex: String, language: String, null, null, null) => new NodeRow(
      `type` = `type`, lex = lex, language = language
    )
    case Row(`type`: String, null, null, null, lex: String, null, datatype: String, null, null) => new NodeRow(
      `type` = `type`, lex = lex, datatype = datatype
    )
    case Row(`type`: String, null, null, null, null, null, null, blankId: String, null) => new NodeRow(
      `type` = `type`, blankId = blankId
    )
    case Row(`type`: String, null, null, null, null, null, null, null, error: String) => new NodeRow(
      `type` = `type`, error = error
    )
    case _ => throw new IllegalArgumentException(s"wrong row shape - $row")
  }
}