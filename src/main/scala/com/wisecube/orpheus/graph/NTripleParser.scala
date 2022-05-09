package com.wisecube.orpheus.graph

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.{Node, NodeFactory, Triple}

import java.net.URI
import scala.language.postfixOps
import scala.util.parsing.combinator.RegexParsers

object NTripleParser extends RegexParsers {
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

  def blankNode: Parser[Node] = ("_:" ~ (pnCharsU | "\\d".r) ~ (((pnChars | "\\.*".r) ~ pnChars)?)) ^^ {
    case _ ~ headChar ~ Some(middleChars ~ finalChar) => NodeFactory.createBlankNode(headChar + middleChars + finalChar)
    case _ ~ headChar ~ None => NodeFactory.createBlankNode(headChar)
  }

  def stringLiteralQuote: Parser[String] = ("\"" ~ (("[^\"\n\r]".r | echar | uchar) *) ~ "\"") ^^ {
    case _ ~ chars ~ _ => chars.mkString
  }

  def iriref: Parser[String] = ("<" ~ (("[^\u0000-\u0020<>\"{}|^`\\\\]".r | uchar) *) ~ ">") ^^ {
    case _ ~ uriChars ~ _ => uriChars.mkString
  }

  def uriNode: Parser[Node] = iriref ^^ { uri => NodeFactory.createURI(uri) }

  def langtag: Parser[String] = ("[a-zA-Z]+".r ~ ("-[a-zA-Z\\d]+".r *)) ^^ {
    case main ~ additional => main + additional.mkString
  }

  def literalNode: Parser[Node] = stringLiteralQuote ~ (("^^" ~ iriref) | "@" ~ langtag) ^^ {
    case lex ~ ("^^" ~ dtURI) => NodeFactory.createLiteral(lex, new XSDDatatype(new URI(dtURI).getFragment))
    case lex ~ ("@" ~ lang) => NodeFactory.createLiteral(lex, lang)
    case _ => throw new IllegalArgumentException("malformed literal")
  }

  def node: Parser[Node] = uriNode | blankNode | literalNode

  def `object`: Parser[Node] = uriNode | blankNode | literalNode

  def predicate: Parser[Node] = uriNode

  def subject: Parser[Node] = uriNode | blankNode

  def triple: Parser[Triple] = (subject ~ predicate ~ `object` ~ ".") ^^ {
    case s ~ p ~ o ~ _ => new Triple(s, p, o)
  }
}
