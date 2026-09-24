package dev.forcetower.hilt.android.dynamic.managers;

import androidx.activity.ComponentActivity;
import dagger.hilt.internal.GeneratedComponentManager;
import dev.forcetower.hilt.android.dynamic.components.DynamicActivityComponent;
import dev.forcetower.hilt.android.dynamic.components.DynamicFeatureComponent;

public final class DynamicActivityComponentManager
        implements GeneratedComponentManager<DynamicActivityComponent> {
    private final ComponentActivity activity;
    private final DynamicFeatureComponent feature;
    private DynamicActivityComponent component;

    public DynamicActivityComponentManager(ComponentActivity activity, DynamicFeatureComponent feature) {
        this.activity = activity;
        this.feature = feature;
    }

    @Override
    public synchronized DynamicActivityComponent generatedComponent() {
        if (component == null) {
            component = DynamicActivityRetainedComponentManager.get(activity, feature)
                    .activityComponent(activity);
        }
        return component;
    }
}
