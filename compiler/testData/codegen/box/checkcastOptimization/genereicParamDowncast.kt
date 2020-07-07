open class A {
    fun foo(): String {
        return "OK"
    }
}

open class B : A()

fun<T : A> test(init: () -> T): T = init()

fun call(b: B ) = b


fun box(): String {
    return call(test { B() }).foo()
}