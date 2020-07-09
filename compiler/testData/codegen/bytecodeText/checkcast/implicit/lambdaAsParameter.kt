fun test(s: () -> Unit) {

}

fun test() {
    test { } //static
    var captured: Any = "123"
    test {
        captured
    } //non-static
}
// JVM_TEMPLATES:
// 0 CHECKCAST

// there is a problem with subtype cheking for lambda class against FunctionX<...>
// JVM_IR_TEMPLATES:
// 2 CHECKCAST