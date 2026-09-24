package dev.forcetower.hilt.android.dynamic

import kotlin.reflect.KClass

/** Generates lifecycle-aware injection for a ComponentActivity or an AndroidX Fragment. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class DynamicAndroidEntryPoint(
    /** Omit with the dynamic Gradle plugin; otherwise extend DynamicHilt_<ClassName> explicitly. */
    val value: KClass<*> = Void::class
)
