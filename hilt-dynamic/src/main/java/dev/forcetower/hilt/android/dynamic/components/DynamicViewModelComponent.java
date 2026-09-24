package dev.forcetower.hilt.android.dynamic.components;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.internal.GeneratedComponent;
import dev.forcetower.hilt.android.dynamic.lifecycle.DynamicViewModelMap;
import java.util.Map;
import javax.inject.Provider;

public interface DynamicViewModelComponent extends GeneratedComponent {
    @DynamicViewModelMap
    Map<Class<?>, Provider<ViewModel>> viewModels();

    interface Builder {
        Builder savedStateHandle(SavedStateHandle handle);
        Builder lifecycle(ViewModelLifecycle lifecycle);
        DynamicViewModelComponent build();
    }
}
