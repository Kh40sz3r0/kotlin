open class SuperBase
open class Base : SuperBase()
open class Derived : Base()

open class BaseGeneric<T: SuperBase> {
    fun test(p: T) : T = p
}

open class DerivedGeneric : BaseGeneric<Derived>()

@JvmField
var property: Base = Base()

fun test(array: Array<Base>) {
    val factory = DerivedGeneric();
    property = factory.test(Derived())
    array[0] = factory.test(Derived())

    var captured = Base();
    {
        captured = factory.test(Derived())
    }()
}

// 0 CHECKCAST SuperBase

// JVM_TEMPLATES:
// 4 CHECKCAST
// 1 CHECKCAST kotlin/jvm/functions/Function0
// 3 CHECKCAST Base

// JVM_IR_TEMPLATES:
// 2 CHECKCAST
// 2 CHECKCAST Base

