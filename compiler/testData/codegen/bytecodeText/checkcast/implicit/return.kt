open class Base
open class Derived : Base()

fun test() : Base {
    return Derived()
}

// 0 CHECKCAST