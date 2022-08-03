package com.wisecube.orpheus.data.datasets

import com.wisecube.orpheus.data.utils
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.jsoup.Jsoup

import java.io.{File, PrintWriter}
import java.util.zip.ZipFile
import scala.collection.JavaConverters._

object ClinicalTrials {
  val aactURLBase = "https://aact.ctti-clinicaltrials.org/static/exported_files/"

  def download(
    period: String = "daily",
    date: String = null,
    filename: String = null
  ): String = {
    val url = "https://aact.ctti-clinicaltrials.org/pipe_files"
    val doc = Jsoup.connect(url).get()
    val downloadURL: String = {
      if (date == null) {
        doc.selectXpath(".//tr/td/a").first().attr("href")
      } else {
        doc.selectXpath(s".//tr[td = '$date']/td/a").attr("href")
      }
    }
    val _filename: String =
      if (filename == null) {
        downloadURL.split("/").last + ".zip"
      } else {
        filename
      }
    utils.fileDownloader(downloadURL, _filename, overwrite = false)
  }

  def unzip(
    path: String,
    directory: String = new File("aact").getAbsolutePath): String = {
    new File(directory).mkdirs()
    val successFile = new File(directory, "_SUCCESS")
    val alreadyExists = {
      if (successFile.exists()) {
        val src = scala.io.Source.fromFile(successFile)
        val foundPath = src.getLines().next()
        src.close()
        foundPath == path
      } else {
        false
      }
    }
    if (!alreadyExists) {
      val zipFile = new ZipFile(path)
      zipFile.entries().asScala.foreach {
        entry =>
          utils.writeStream(zipFile.getInputStream(entry), new File(directory, entry.getName).getAbsolutePath)
      }
      val printer = new PrintWriter(successFile)
      printer.println(path)
      printer.close()
    }
    directory
  }

  def getFiles(directory: String = new File("aact").getAbsolutePath): Seq[String] =
    new File(directory).list().filter(_.endsWith(".txt")).sorted

  def load(tableFile: String, directory: String = new File("aact").getAbsolutePath): DataFrame = {
    val spark = SparkSession.builder.getOrCreate()
    spark.read.option("inferSchema", true).option("delimiter", "|").option("header", true)
      .csv(new File(directory, tableFile).getAbsolutePath)
  }
}
