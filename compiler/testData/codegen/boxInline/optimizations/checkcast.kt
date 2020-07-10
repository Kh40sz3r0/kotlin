// FILE: 1.kt
package test

open class Base

open class Derived : Base()
open class Derived2 : Base()

inline fun test(s: () -> Base): Base {
    return s()
}

fun cond() = true

// FILE: 2.kt

import test.*

fun base(base: Base) {

}

fun box(): String {
    val value = if (cond())
        test { Derived() }
    else test { Derived2() }
    base(value)
    return "OK"
}

