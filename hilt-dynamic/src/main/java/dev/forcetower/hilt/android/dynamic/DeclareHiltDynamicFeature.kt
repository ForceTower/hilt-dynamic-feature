package dev.forcetower.hilt.android.dynamic

import dagger.hilt.GeneratesRootInput
import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@GeneratesRootInput
annotation class DeclareHiltDynamicFeature(
    val dependencies: Array<KClass<*>> = []
)