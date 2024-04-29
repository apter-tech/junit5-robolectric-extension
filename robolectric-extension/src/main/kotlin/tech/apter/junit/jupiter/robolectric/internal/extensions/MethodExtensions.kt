package tech.apter.junit.jupiter.robolectric.internal.extensions

import java.lang.reflect.Method

@Suppress("ReturnCount")
internal fun Method.hasTheSameParameterTypes(method: Method): Boolean {
    if (parameterTypes.size != method.parameterTypes.size) {
        return false
    }

    for (i in parameterTypes.indices) {
        val clazz1 = parameterTypes[i]
        val clazz2 = method.parameterTypes[i]

        if (clazz1.name != clazz2.name) {
            return false
        }
    }
    return true
}
