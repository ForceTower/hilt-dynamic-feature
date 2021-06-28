package dev.forcetower.hilt.android.dynamic.components
import dagger.hilt.DefineComponent
import dagger.hilt.components.SingletonComponent
import dev.forcetower.hilt.android.dynamic.scopes.DynamicScope

@DynamicScope
@DefineComponent(parent = SingletonComponent::class)
interface DynamicFeatureComponent