interface FirDeclaration
interface FirSymbolOwner<E> where E : FirSymbolOwner<E>, E : FirDeclaration

abstract class AbstractFirBasedSymbol<E> : FirSymbolOwner<E> where E : FirSymbolOwner<E>, E : FirDeclaration {
    lateinit var fir: E
}

open class FirClassSymbol<E : FirClass<E>>: AbstractFirBasedSymbol<E>()

open class FirClass<F : FirClass<F>> : FirSymbolOwner<F>, FirDeclaration
open class FirRegularClass : FirClass<FirRegularClass>()
open class ClassFirBasedSymbol : AbstractFirBasedSymbol<FirRegularClass>()

fun test(symbol: ClassFirBasedSymbol) {
    test(symbol.fir)
}

fun test(p: FirRegularClass) {

}

fun box(): String {
    val classFirBasedSymbol = ClassFirBasedSymbol()
    classFirBasedSymbol.fir = FirRegularClass()
    test(classFirBasedSymbol)
    return "OK"
}