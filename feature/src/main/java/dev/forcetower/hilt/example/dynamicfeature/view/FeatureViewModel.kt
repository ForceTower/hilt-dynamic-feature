package dev.forcetower.hilt.example.dynamicfeature.view

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ViewModelLifecycle
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import dev.forcetower.hilt.example.dynamicfeature.injection.FeatureSession
import dev.forcetower.hilt.example.dynamicfeature.injection.RetainedState
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class FeatureViewModel @Inject constructor(
    val session: FeatureSession,
    val retainedState: RetainedState,
    val state: SavedStateHandle,
    val first: ViewModelState,
    val second: ViewModelState,
    @param:Named("viewModelLabel") val label: String
) : ViewModel()

@ViewModelScoped
class ViewModelState @Inject constructor(lifecycle: ViewModelLifecycle) {
    var cleared = false
        private set

    init {
        lifecycle.addOnClearedListener { cleared = true }
    }
}

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelBindings {
    @Provides
    @Named("viewModelLabel")
    fun label(): String = "ViewModel injected"
}
