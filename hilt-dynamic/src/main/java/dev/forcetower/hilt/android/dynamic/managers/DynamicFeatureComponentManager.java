package dev.forcetower.hilt.android.dynamic.managers;

import dagger.hilt.android.internal.managers.ComponentSupplier;
import dagger.hilt.internal.GeneratedComponentManager;

/**
 * Do not use except in Hilt generated code!
 *
 * <p>A manager for the creation of components that live in the Application.
 */
public final class DynamicFeatureComponentManager implements GeneratedComponentManager<Object> {
    private volatile Object component;
    private final Object componentLock = new Object();
    private final ComponentSupplier componentCreator;

    public DynamicFeatureComponentManager(ComponentSupplier componentCreator) {
        this.componentCreator = componentCreator;
    }

    @Override
    public Object generatedComponent() {
        if (component == null) {
            synchronized (componentLock) {
                if (component == null) {
                    component = componentCreator.get();
                }
            }
        }
        return component;
    }
}
