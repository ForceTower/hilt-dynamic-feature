package dev.forcetower.hilt.android.dynamic.components;

import android.app.Activity;
import dagger.hilt.internal.GeneratedComponent;

public interface DynamicActivityComponent extends GeneratedComponent {
    DynamicFragmentComponent.Builder fragmentComponentBuilder();
    DynamicViewModelComponent.Builder viewModelComponentBuilder();

    interface Builder {
        Builder activity(Activity activity);
        DynamicActivityComponent build();
    }
}
