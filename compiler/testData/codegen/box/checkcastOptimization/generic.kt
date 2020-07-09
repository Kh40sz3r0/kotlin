// TARGET_BACKEND: JVM
// WITH_RUNTIME
open class TransformationInfo
class MyTransformationInfo(val value: String) : TransformationInfo()
abstract class ObjectTransformer<out T : TransformationInfo>(@JvmField val transformationInfo: T)

class MyTransformer(info: MyTransformationInfo): ObjectTransformer<MyTransformationInfo>(info) {

    fun test(): String {
        return test(transformationInfo)
    }

    fun test(info: MyTransformationInfo): String {
        return info.value
    }
}

fun box(): String {
    return MyTransformer(MyTransformationInfo("OK")).test()
}