package com.graphster.orpheus.config.graph

import com.graphster.orpheus.config.{AtomicValue, Configuration, ValueConf, types}
import com.graphster.orpheus.config.types.{BooleanFieldType, ConfFieldType, MetadataField, StringFieldType}
import org.apache.commons.codec.binary.Hex
import org.apache.jena.graph.{Node, Node_Ext}
import org.apache.spark.sql.{Column, Row, functions => sf}

import java.nio.charset.Charset
import scala.language.implicitConversions

case class ErrorGraphConf(name: String, errorMsg: ValueConf with AtomicValue, encodeMsg: Boolean = true)
  extends NodeConf(
    Configuration(
      ValueConf.NameKey -> MetadataField(name),
      NodeConf.ErrorKey -> MetadataField(errorMsg),
      ErrorGraphConf.EncodeMsgKey -> MetadataField(encodeMsg)
    )) {
  override def toColumn: Column = {
    val errorURI = if (encodeMsg) {
      sf.concat_ws(
        ErrorGraphConf.ErrorSep,
        sf.lit(ErrorGraphConf.ErrorNamespace),
        sf.sha1(errorMsg.toColumn)
      ).as(name, metadata)
    } else {
      sf.concat_ws(
        ErrorGraphConf.ErrorSep,
        sf.lit(ErrorGraphConf.ErrorNamespace),
        errorMsg.toColumn
      ).as(name, metadata)
    }
    URIGraphConf.uri2row(errorURI)
  }

  override def keys: Set[String] = Set(ValueConf.NameKey, NodeConf.ErrorKey, ErrorGraphConf.EncodeMsgKey)

  override def keyTypes: Map[String, types.MetadataFieldType] = Map(
    ValueConf.NameKey -> StringFieldType,
    NodeConf.ErrorKey -> ConfFieldType,
    ErrorGraphConf.EncodeMsgKey -> BooleanFieldType
  )

  override def get(key: String): MetadataField[_] = key match {
    case ValueConf.NameKey => MetadataField(name)
    case NodeConf.ErrorKey => MetadataField(errorMsg)
    case ErrorGraphConf.EncodeMsgKey => MetadataField(encodeMsg)
    case _ => throw new NoSuchElementException()
  }
}

object ErrorGraphConf extends NodeConfBuilder {
  private val ErrorNamespace: String = "https://schema.org/error"
  private val ErrorSep: String = "#"
  override val NodeType: String = "Node_Error"
  val EncodeMsgKey: String = "hashMsg"

  private implicit class WithError(node: Node) {
    def error: Throwable = node match {
      case ne: Node_Error => ne.error
      case _ => null: Throwable
    }
  }

  private def encodeMessage(msg: String): String = Hex.encodeHexString(msg.getBytes(Charset.forName("UTF-8")))

  private def decodeMessage(msgHex: String): String = new String(Hex.decodeHex(msgHex), Charset.forName("UTF-8"))

  private def getErrorURI(msgHex: String): String = ErrorNamespace + ErrorSep + msgHex

  private def getMessageHex(uri: String): String = uri.stripSuffix(ErrorNamespace + ErrorSep)

  def parts2jena(throwable: Throwable): Node = Node_Error(throwable)

  def parts2jena(msg: String): Node = Node_Error(new Error(msg))

  override def jena2row(elem: Node): Row = buildRow(error = encodeMessage(elem.error.getMessage))

  override def row2jena(row: Row): Node = Option(row.getAs[String](NodeConf.ErrorKey)) match {
    case Some(msg) => parts2jena(msg)
    case _ => throw new IllegalArgumentException(s"Incorrect schema (${row.toSeq}) - $row")
  }

  override def jena2string(elem: Node): String = getErrorURI(encodeMessage(elem.error.getMessage))

  override def string2jena(str: String): Node = parts2jena(decodeMessage(getMessageHex(str)))

  override def apply(config: Configuration): ErrorGraphConf =
    new ErrorGraphConf(
      config.getString(ValueConf.NameKey),
      AtomicValue.fromConfiguration(config.getConf(NodeConf.ErrorKey)),
      config.getBoolean(ErrorGraphConf.EncodeMsgKey)
    )
}

case class Node_Error(error: Throwable) extends Node_Ext[Throwable](error)