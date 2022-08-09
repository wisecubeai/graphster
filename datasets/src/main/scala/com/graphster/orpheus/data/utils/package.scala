package com.graphster.orpheus.data

import java.io.{File, InputStream, PrintWriter}
import java.net.URL
import scala.language.postfixOps
import scala.sys.process._

package object utils {
  def fileDownloader(url: String, filename: String, overwrite: Boolean = true): String = {
    if (overwrite || !new File(filename).exists()) {
      new URL(url) #> new File(filename) !!
    }
    filename
  }

  def fileCat(url: String): Stream[String] = new URL(url).cat.lineStream

  def writeStream(is: InputStream, filename: String): String = {
    val printer = new PrintWriter(filename)
    scala.io.Source.fromInputStream(is).getLines().foreach(printer.println)
    printer.close()
    filename
  }
}
