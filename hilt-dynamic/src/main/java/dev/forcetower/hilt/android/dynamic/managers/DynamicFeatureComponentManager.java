package dev.forcetower.hilt.android.dynamic.managers;

import android.app.Application;
import android.content.Context;
import java.util.IdentityHashMap;
import java.util.Map;
import dev.forcetower.hilt.android.dynamic.components.DynamicFeatureComponent;

public final class DynamicFeatureComponentManager {
    public interface Factory {
        DynamicFeatureComponent create(Application application);
    }

    private final Map<Application, DynamicFeatureComponent> components = new IdentityHashMap<>();

    public synchronized DynamicFeatureComponent get(Context context, Factory factory) {
        Context application = context.getApplicationContext();
        if (!(application instanceof Application)) {
            throw new IllegalStateException("Dynamic Hilt requires an Application context.");
        }
        Application owner = (Application) application;
        DynamicFeatureComponent component = components.get(owner);
        if (component == null) {
            component = factory.create(owner);
            components.put(owner, component);
        }
        return component;
    }
}
