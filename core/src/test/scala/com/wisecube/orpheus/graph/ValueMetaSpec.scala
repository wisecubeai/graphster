package com.wisecube.orpheus.graph

import org.scalatest.funsuite.AnyFunSuite

class ValueMetaSpec extends AnyFunSuite {

  test("Testing rebuilding from metadata") {
    val emptyValueMeta = EmptyValueMeta()
    val literalValueMeta = LiteralValueMeta("literal", "abc")
    val columnValueMeta = ColumnValueMeta("column", "a")
    val fallbackValueMeta = FallbackValueMeta("fallback", Seq(columnValueMeta, literalValueMeta), emptyValueMeta)
    val concatValueMeta = ConcatValueMeta("concatenated", Seq(literalValueMeta, fallbackValueMeta))
    assertResult(emptyValueMeta)(ValueMeta.fromMetadata(emptyValueMeta.toMetadata))
    assertResult(literalValueMeta)(ValueMeta.fromMetadata(literalValueMeta.toMetadata))
    assertResult(columnValueMeta)(ValueMeta.fromMetadata(columnValueMeta.toMetadata))
    assertResult(fallbackValueMeta)(ValueMeta.fromMetadata(fallbackValueMeta.toMetadata))
    assertResult(concatValueMeta)(ValueMeta.fromMetadata(concatValueMeta.toMetadata))
  }
}
