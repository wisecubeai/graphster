package com.wisecube.orpheus.extraction.io

import org.apache.spark.sql.DataFrame

object RDFData {
  def load(location: String, format: String = "NTriples"): DataFrame = ???
}
