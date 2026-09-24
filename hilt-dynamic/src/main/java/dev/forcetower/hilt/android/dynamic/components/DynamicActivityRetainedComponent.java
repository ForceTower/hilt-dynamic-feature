package dev.forcetower.hilt.android.dynamic.components;

import dagger.hilt.internal.GeneratedComponent;
import dagger.hilt.android.ActivityRetainedLifecycle;

public interface DynamicActivityRetainedComponent extends GeneratedComponent {
    DynamicActivityComponent.Builder activityComponentBuilder();
    DynamicViewModelComponent.Builder viewModelComponentBuilder();

    interface Builder {
        Builder lifecycle(ActivityRetainedLifecycle lifecycle);
        DynamicActivityRetainedComponent build();
    }
}
