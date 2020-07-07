var result = ""

open class Base

open class Derived : Base() {
    fun test(base: Base, base2: Base) {
        result += "O"
    }
}

open class Child1 : Derived()
open class Child2 : Derived()

fun Derived.testExt(base: Base, base2: Base) {
    result += "K"
}


fun box(): String {
    Child2().test(try { Child1() } finally { Child2()} , try { Child1() } finally { Child2() } )
    Child2().testExt(try { Child1() } finally { Child2() } , try { Child1() } finally { Child2() } )

    return result
}
