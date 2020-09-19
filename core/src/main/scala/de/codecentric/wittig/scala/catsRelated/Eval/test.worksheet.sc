val a = 10
val b = 2 + a

case class Person(name: String, alter: Int)

val p = Person("lkjl", 33)

List.tabulate(4)(_ * 3)

val i: Int = 1
val one: 1 = 1

val hello: "hello" = "hello"

type OrderedView[T] = T => Ordered[T]
def f2[T: OrderedView](a: T, b: T) = a < b

f2(3, 2)
