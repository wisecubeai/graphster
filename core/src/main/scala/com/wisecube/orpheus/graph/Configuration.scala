package com.wisecube.orpheus.graph

import com.google.gson.JsonParser
import org.apache.spark.sql.types.Metadata

import java.io.FileNotFoundException
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

final class Configuration(private val config: Map[String, Metadata]) {
  def get[T <: ValueMeta: ClassTag](name: String): T = getOption(name) match {
    case Some(m) => m
    case None => throw new NoSuchElementException(s"No ValueMeta with name $name")
  }

  def getOption[T <: ValueMeta: ClassTag](name: String): Option[T] = config.get(name).map(ValueMeta.fromMetadata) match {
    case Some(t) if implicitly[ClassTag[T]].runtimeClass.isInstance(t) => Some(t.asInstanceOf[T])
    case Some(m) => throw new ClassCastException(s"Incorrect ValueMeta type - ${m.getClass}")
    case None => None
  }
}

object Configuration {
  def apply(config: Map[String, Metadata]): Configuration = new Configuration(config)

  def apply(config: (String, Metadata)*): Configuration = new Configuration(Map(config: _*))

  def load(filepath: String): Configuration = {
    val path = Paths.get(filepath)
    if (Files.exists(path)) {
      val reader = Files.newBufferedReader(path)
      val json = JsonParser.parseReader(reader).getAsJsonObject
      val config: Map[String, Metadata] = json.entrySet().asScala.map(e => (e.getKey, Metadata.fromJson(e.getValue.toString))).toMap
      Configuration(config)
    } else {
      throw new FileNotFoundException(s"File note found - $filepath")
    }
  }
}