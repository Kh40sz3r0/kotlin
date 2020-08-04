fun foo(y: Y) {}

open class Y

class Z : Y() {
    fun bar() {
        foo(this)
    }
}

// 0 CHECKCAST