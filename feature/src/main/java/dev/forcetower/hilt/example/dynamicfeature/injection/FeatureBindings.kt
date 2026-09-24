package dev.forcetower.hilt.example.dynamicfeature.injection

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.EntryPoint
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.android.scopes.FragmentScoped
import dagger.hilt.android.ActivityRetainedLifecycle
import dev.forcetower.hilt.android.dynamic.components.DynamicFeatureComponent
import dev.forcetower.hilt.android.dynamic.scopes.DynamicScope
import dev.forcetower.hilt.example.Token
import javax.inject.Inject
import javax.inject.Named

@DynamicScope
class FeatureSession @Inject constructor(
    val token: Token,
    @param:ApplicationContext val context: Context
)

@ActivityRetainedScoped
class RetainedState @Inject constructor(lifecycle: ActivityRetainedLifecycle) {
    var cleared = false
        private set

    init {
        lifecycle.addOnClearedListener { cleared = true }
    }
}

@ActivityScoped
class ScreenState @Inject constructor(@param:ActivityContext val context: Context)

@FragmentScoped
class FragmentState @Inject constructor()

@EntryPoint
@InstallIn(DynamicFeatureComponent::class)
interface FeatureAccess {
    fun session(): FeatureSession
}

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ScreenAccess {
    fun screen(): ScreenState
}

@Module
@InstallIn(DynamicFeatureComponent::class)
object FeatureBindings {
    @Provides
    @Named("featureLabel")
    fun label(@Named("applicationName") name: String): String = "$name feature injected"
}
