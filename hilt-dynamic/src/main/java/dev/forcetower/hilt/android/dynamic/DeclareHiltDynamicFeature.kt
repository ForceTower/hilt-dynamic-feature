package dev.forcetower.hilt.android.dynamic

import kotlin.reflect.KClass

/** Declares the feature graph and the base app's SingletonComponent entry points it depends on. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class DeclareHiltDynamicFeature(
    val dependencies: Array<KClass<*>> = []
)
