package dev.forcetower.hilt.android.dynamic.components

import dagger.hilt.internal.GeneratedComponent

/** Feature-local root. Use @InstallIn(DynamicFeatureComponent::class) with @DynamicScope. */
interface DynamicFeatureComponent : GeneratedComponent {
    fun retainedComponentBuilder(): DynamicActivityRetainedComponent.Builder
}
