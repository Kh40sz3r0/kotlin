fun cond() = true

var result = ""

open class Base

open class Derived : Base() {
    fun test(base: Base, base2: Base) {
        result += "O"
    }
}

open class Child1 : Derived()
open class Child2 : Derived()

fun Derived.test2(base: Base, base2: Base) {
    result += "K"
}

fun test(child1: Child1?,child2: Child2) {
    (child1 ?: child2).test(child1 ?: child2, child1 ?: child2)
    (child1 ?: child2).test2(child1 ?: child2, child1 ?: child2)
}

fun box(): String {
    test(Child1(), Child2())
    return result
}
