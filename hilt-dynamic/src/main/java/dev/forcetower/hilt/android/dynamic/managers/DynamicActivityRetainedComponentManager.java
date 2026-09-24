package dev.forcetower.hilt.android.dynamic.managers;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import dev.forcetower.hilt.android.dynamic.components.DynamicActivityComponent;
import dev.forcetower.hilt.android.dynamic.components.DynamicActivityRetainedComponent;
import dev.forcetower.hilt.android.dynamic.components.DynamicFeatureComponent;
import dev.forcetower.hilt.android.dynamic.lifecycle.DynamicRetainedLifecycle;

final class DynamicActivityRetainedComponentManager {
    private DynamicActivityRetainedComponentManager() {}

    static RetainedComponent get(
            ComponentActivity activity, DynamicFeatureComponent feature) {
        return new ViewModelProvider(activity, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return modelClass.cast(new RetainedComponent(feature));
            }
        }).get(RetainedComponent.class.getName() + ":" + feature.getClass().getName(), RetainedComponent.class);
    }

    static final class RetainedComponent extends ViewModel {
        final DynamicActivityRetainedComponent component;
        private final DynamicRetainedLifecycle lifecycle = new DynamicRetainedLifecycle();
        private ComponentActivity currentActivity;
        private DynamicActivityComponent activityComponent;

        RetainedComponent(DynamicFeatureComponent feature) {
            this.component = feature.retainedComponentBuilder().lifecycle(lifecycle).build();
        }

        @Override
        protected void onCleared() {
            currentActivity = null;
            activityComponent = null;
            lifecycle.dispatchOnCleared();
        }

        synchronized DynamicActivityComponent activityComponent(ComponentActivity activity) {
            if (currentActivity != activity) {
                currentActivity = activity;
                activityComponent = component.activityComponentBuilder().activity(activity).build();
                activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                    @Override
                    public void onDestroy(@NonNull LifecycleOwner owner) {
                        synchronized (RetainedComponent.this) {
                            if (currentActivity == owner) {
                                currentActivity = null;
                                activityComponent = null;
                            }
                        }
                        owner.getLifecycle().removeObserver(this);
                    }
                });
            }
            return activityComponent;
        }
    }
}
