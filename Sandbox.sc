import scala.collection.mutable

case class Foo(x: Int, y: String, z: Seq[String])

val foo1 = Foo(1, "a", Seq("Alex", "Thomas"))
val foo2 = Foo(1, "a", Array("Alex", "Thomas"))
val foo3 = Foo(1, "a", mutable.ArraySeq("Alex", "Thomas"))

foo1.hashCode()
foo2.hashCode()
foo3.hashCode()
(foo1.x, foo1.y, foo1.z).##