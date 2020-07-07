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


fun box(): String {
    (if (cond()) Child1() else Child2()).test(if (cond()) Child1() else Child2(), if (cond()) Child1() else Child2())
    (if (cond()) Child1() else Child2()).test2(if (cond()) Child1() else Child2(), if (cond()) Child1() else Child2())

    return result
}
