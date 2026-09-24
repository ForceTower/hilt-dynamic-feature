package dev.forcetower.hilt.android.dynamic.managers;

import androidx.fragment.app.Fragment;
import androidx.activity.ComponentActivity;
import dagger.hilt.internal.GeneratedComponentManager;
import dev.forcetower.hilt.android.dynamic.components.DynamicActivityComponent;
import dev.forcetower.hilt.android.dynamic.components.DynamicFragmentComponent;
import dev.forcetower.hilt.android.dynamic.components.DynamicFeatureComponent;

public final class DynamicFragmentComponentManager
        implements GeneratedComponentManager<DynamicFragmentComponent> {
    private final Fragment fragment;
    private final DynamicFeatureComponent feature;
    private DynamicFragmentComponent component;

    public DynamicFragmentComponentManager(Fragment fragment, DynamicFeatureComponent feature) {
        this.fragment = fragment;
        this.feature = feature;
    }

    @Override
    public synchronized DynamicFragmentComponent generatedComponent() {
        if (component == null) {
            ComponentActivity activity = fragment.requireActivity();
            DynamicActivityComponent parent = DynamicActivityRetainedComponentManager
                    .get(activity, feature).activityComponent(activity);
            component = parent
                    .fragmentComponentBuilder().fragment(fragment).build();
        }
        return component;
    }
}
