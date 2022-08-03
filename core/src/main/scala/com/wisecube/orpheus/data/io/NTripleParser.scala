package com.wisecube.orpheus.data.io

import com.wisecube.orpheus.Utils.trycall
import com.wisecube.orpheus.config.graph._
import org.apache.jena.graph.{Node, Triple}

import scala.util.parsing.combinator.RegexParsers

object NTripleParser extends RegexParsers {
  override def skipWhitespace = false

  def hex: Parser[String] = """\d|[A-F]|[a-f]""".r

  def pnCharsBase: Parser[String] =
    "[A-Z]|[a-z]".r | "[À-Ö]".r | "[Ø-ö]".r | "[ø-˿]".r | "[Ͱ-ͽ]".r | "[Ϳ-\u1FFF]".r | "[\u200C-\u200D]".r |
      "[⁰-\u218F]".r | "[Ⰰ-\u2FEF]".r | "[、-\uD7FF]".r | "[豈-\uFDCF]".r | "[ﷰ-�]".r

  def pnCharsU: Parser[String] = pnCharsBase | "[_:]".r

  def pnChars: Parser[String] = pnCharsU | "-" | "\\d".r | "·" | "[\u0300-\u036F]".r | "[ȃ-⁀]".r

  def echar: Parser[String] = """"[tbnrf"'\\]""".r

  def uchar: Parser[String] = (("\\u" ~ repN(4, hex)) | ("\\U" ~ repN(6, hex))) ^^ {
    case prefix ~ bytes => prefix + bytes.mkString
  }

  def blankNode: Parser[Node] = ("_:" ~ (pnCharsU | "\\d".r) ~ (((pnChars | "\\.*".r) ~ pnChars) ?)) ^^ {
    case _ ~ headChar ~ Some(middleChars ~ finalChar) => BlankGraphConf.parts2jena(headChar + middleChars + finalChar)
    case _ ~ headChar ~ None => BlankGraphConf.parts2jena(headChar)
  }

  def stringLiteralQuote: Parser[String] = ("\"" ~ (("\\\"" | "[^\"\n\r]".r | echar | uchar) *) ~ "\"") ^^ {
    case _ ~ chars ~ _ => chars.mkString.replace("\\\\", "\\")
  }

  def iriref: Parser[String] = ("<" ~ (("[^\u0000-\u0020<>\"{}|^`\\\\]".r | uchar) *) ~ ">") ^^ {
    case _ ~ uriChars ~ _ => uriChars.mkString
  }

  def uriNode: Parser[Node] = iriref ^^ { uri => URIGraphConf.parts2jena(uri) }

  def langtag: Parser[String] = ("[a-zA-Z]+".r ~ ("-[a-zA-Z\\d]+".r *)) ^^ {
    case main ~ additional => main + additional.mkString
  }

  def literalNode: Parser[Node] = stringLiteralQuote ~ (("^^" ~ iriref) | "@" ~ langtag) ^^ {
    case lex ~ ("^^" ~ dtURI) => DataLiteralGraphConf.parts2jena(lex, dtURI)
    case lex ~ ("@" ~ lang) => LangLiteralGraphConf.parts2jena(lex, lang)
    case _ => throw new IllegalArgumentException("malformed literal")
  }

  def node: Parser[Node] = uriNode | blankNode | literalNode

  def `object`: Parser[Node] = uriNode | blankNode | literalNode

  def predicate: Parser[Node] = uriNode

  def subject: Parser[Node] = uriNode | blankNode

  def triple: Parser[Triple] = (subject ~ "\\s+".r ~ predicate ~ "\\s+".r ~ `object` ~ "\\s+\\.".r) ^^ {
    case s ~ _ ~ p ~ _ ~ o ~ _ => new Triple(s, p, o)
  }

  val fallbackNodeParser: String => Node =
    trycall(NodeConf.string2jena _) { case error: Throwable => ErrorGraphConf.parts2jena(error) }

  def fallbackTripleParser(str: String): Triple = {
    val Array(subjectStr, remainderStr) = str.stripSuffix(".").trim.split("\\s+", 2)
    val Array(predicateStr, objectStr) = remainderStr.split("\\s+", 2)
    val subject = fallbackNodeParser(subjectStr)
    val predicate = fallbackNodeParser(predicateStr)
    val `object` = fallbackNodeParser(objectStr)
    new Triple(subject, predicate, `object`)
  }
}
