package com.graphster.orpheus.config

package object types {
  sealed trait MetadataFieldType

  case object LongFieldType extends MetadataFieldType

  case object DoubleFieldType extends MetadataFieldType

  case object BooleanFieldType extends MetadataFieldType

  case object StringFieldType extends MetadataFieldType

  case object ConfFieldType extends MetadataFieldType

  case object LongSeqFieldType extends MetadataFieldType

  case object DoubleSeqFieldType extends MetadataFieldType

  case object BooleanSeqFieldType extends MetadataFieldType

  case object StringSeqFieldType extends MetadataFieldType

  case object ConfSeqFieldType extends MetadataFieldType

  sealed trait MetadataField[T] {
    def value: T

    val fieldType: MetadataFieldType
  }

  object MetadataField {
    def apply(value: Long): MetadataField[Long] = LongField(value)
    def apply(value: Double): MetadataField[Double] = DoubleField(value)
    def apply(value: Boolean): MetadataField[Boolean] = BooleanField(value)
    def apply(value: String): MetadataField[String] = StringField(value)
    def apply(value: Conf): MetadataField[Conf] = ConfField(value)
  }

  case class LongField(value: Long) extends MetadataField[Long] {
    override val fieldType: MetadataFieldType = LongFieldType
  }

  case class DoubleField(value: Double) extends MetadataField[Double] {
    override val fieldType: MetadataFieldType = DoubleFieldType
  }

  case class BooleanField(value: Boolean) extends MetadataField[Boolean] {
    override val fieldType: MetadataFieldType = BooleanFieldType
  }

  case class StringField(value: String) extends MetadataField[String] {
    override val fieldType: MetadataFieldType = StringFieldType
  }

  case class ConfField(value: Conf) extends MetadataField[Conf] {
    override val fieldType: MetadataFieldType = ConfFieldType
  }

  case class LongSeqField(value: Seq[Long]) extends MetadataField[Seq[Long]] {
    override val fieldType: MetadataFieldType = LongSeqFieldType
  }

  case class DoubleSeqField(value: Seq[Double]) extends MetadataField[Seq[Double]] {
    override val fieldType: MetadataFieldType = DoubleSeqFieldType
  }

  case class BooleanSeqField(value: Seq[Boolean]) extends MetadataField[Seq[Boolean]] {
    override val fieldType: MetadataFieldType = BooleanSeqFieldType
  }

  case class StringSeqField(value: Seq[String]) extends MetadataField[Seq[String]] {
    override val fieldType: MetadataFieldType = StringSeqFieldType
  }

  case class ConfSeqField(value: Seq[Conf]) extends MetadataField[Seq[Conf]] {
    override val fieldType: MetadataFieldType = ConfSeqFieldType
  }
}
