package tech.apter.junit.jupiter.robolectric.internal.extensions

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

internal fun Class<*>.isExtendedWithRobolectric(): Boolean {
    val isExtended = findAnnotations(ExtendWith::class.java).any { annotation ->
        val valueMethod = annotation.javaClass.getDeclaredMethod("value")

        @Suppress("UNCHECKED_CAST")
        val value: Array<Class<*>> = valueMethod.invoke(annotation) as Array<Class<*>>
        value.any { it.name == RobolectricExtension::class.java.name }
    }
    return if (isExtended) {
        true
    } else if (declaringClass != null) {
        declaringClass.isExtendedWithRobolectric()
    } else {
        false
    }
}

internal inline val Class<*>.isNested: Boolean get() = declaringClass != null
internal inline val Class<*>.isNestedTest: Boolean
    get() = isNested && findAnnotations(type = Nested::class.java).isNotEmpty()

internal tailrec fun Class<*>.nearestOuterNestedTestOrOuterMostDeclaringClass(): Class<*> {
    return if (declaringClass == null || isNestedTest) {
        this
    } else if (declaringClass.isNestedTest) {
        declaringClass
    } else {
        declaringClass.nearestOuterNestedTestOrOuterMostDeclaringClass()
    }
}

internal tailrec fun Class<*>.outerMostDeclaringClass(): Class<*> {
    return if (declaringClass == null) {
        this
    } else {
        return declaringClass.outerMostDeclaringClass()
    }
}

private fun Class<*>.findAnnotations(type: Class<*>): List<Annotation> {
    return annotations.filter { annotation ->
        annotation.annotationClass.java.name == type.name
    }
}
