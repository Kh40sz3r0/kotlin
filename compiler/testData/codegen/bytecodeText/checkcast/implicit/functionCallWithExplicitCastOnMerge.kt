fun cond() = true

open class Base
open class Derived : Base() {
    fun test(base: Base, base2: Base) {

    }
}
open class Child1 : Derived()
open class Child2 : Derived()

fun Derived.test2(base: Base, base2: Base) {

}


fun test() {
    (if (cond()) Child1() else Child2()).test(if (cond()) Child1() else Child2(), if (cond()) Child1() else Child2())
    (if (cond()) Child1() else Child2()).test2(if (cond()) Child1() else Child2(), if (cond()) Child1() else Child2())
}

// 12 CHECKCAST
// 12 CHECKCAST Derived