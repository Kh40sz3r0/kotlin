open class Base
open class Derived : Base()

@JvmField
var property: Base = Base()

fun test(array: Array<Base>) {
    property = Derived() as Derived
    property = Derived() as Base
    array[0] = Derived() as Derived
    array[0] = Derived() as Base

    var captured = Base() as Base;
    {
        captured = Derived() as Derived
        captured = Derived() as Base
    }()
}

// JVM_TEMPLATES:
// 4 CHECKCAST
// 1 CHECKCAST kotlin/jvm/functions/Function0

// This checkcast should not be generated
// 3 CHECKCAST Base

// JVM_IR_TEMPLATES:
// 0 CHECKCAST
