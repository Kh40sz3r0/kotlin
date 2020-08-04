class Q<TT> {
    fun <T> qz(x: T, block: (T) -> String) = block(x)

    fun <TTT> problematic(y: TTT): String {
        class CC

        return qz(CC::class) { "OK" }
    }
}

fun box() = Q<Int>().problematic(33)
