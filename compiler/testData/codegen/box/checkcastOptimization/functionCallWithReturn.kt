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
    (if (cond()) Child1() else return "fail 0").test(if (cond()) Child1() else return "fail 1", if (cond()) Child1() else return "fail 2")
    (if (cond()) Child1() else return "fail 00").test2(if (cond()) Child1() else return "fail 3", if (cond()) Child1() else return "fail 4")

    return result
}
