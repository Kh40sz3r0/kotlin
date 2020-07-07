interface A
open class Base
open class Derived : Base()
open class Derived2 : Base()

fun cond(): Boolean = true

@JvmField
var property: Base = Base()

fun test(array: Array<Base>) {
    property = if (cond()) Derived() else Derived2()
    array[0] = if (cond()) Derived() else Derived2()

    var captured = Base();
    {
        captured = if (cond()) Derived() else Derived2()
    }()
}

// JVM_TEMPLATES:
// 7 CHECKCAST
// 1 CHECKCAST kotlin/jvm/functions/Function0
// 6 CHECKCAST Base

// JVM_IR_TEMPLATES:
// 6 CHECKCAST
// 6 CHECKCAST Base

