package dev.forcetower.hilt.android.dynamic.lifecycle;

import androidx.annotation.NonNull;
import androidx.lifecycle.SavedStateHandleSupport;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.CreationExtras;
import dev.forcetower.hilt.android.dynamic.components.DynamicViewModelComponent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class DynamicViewModelFactory implements ViewModelProvider.Factory {
    private final Set<Class<?>> modelClasses;
    private final DynamicViewModelComponent.Builder componentBuilder;
    private final ViewModelProvider.Factory delegate;

    public DynamicViewModelFactory(Class<?>[] modelClasses,
            DynamicViewModelComponent.Builder componentBuilder, ViewModelProvider.Factory delegate) {
        this.modelClasses = new HashSet<>(Arrays.asList(modelClasses));
        this.componentBuilder = componentBuilder;
        this.delegate = delegate;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass, @NonNull CreationExtras extras) {
        if (!modelClasses.contains(modelClass)) {
            return delegate.create(modelClass, extras);
        }
        DynamicRetainedLifecycle lifecycle = new DynamicRetainedLifecycle();
        DynamicViewModelComponent component = componentBuilder
                .savedStateHandle(SavedStateHandleSupport.createSavedStateHandle(extras))
                .lifecycle(lifecycle).build();
        T model = modelClass.cast(component.viewModels().get(modelClass).get());
        model.addCloseable(lifecycle::dispatchOnCleared);
        return model;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClasses.contains(modelClass)) {
            throw new IllegalStateException("Use ViewModelProvider with CreationExtras for a Hilt ViewModel.");
        }
        return delegate.create(modelClass);
    }
}
