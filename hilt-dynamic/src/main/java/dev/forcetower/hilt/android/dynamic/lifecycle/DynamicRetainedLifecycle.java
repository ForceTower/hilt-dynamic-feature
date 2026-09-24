package dev.forcetower.hilt.android.dynamic.lifecycle;

import android.os.Looper;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.lifecycle.RetainedLifecycle;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DynamicRetainedLifecycle implements ActivityRetainedLifecycle, ViewModelLifecycle {
    private final Set<RetainedLifecycle.OnClearedListener> listeners = new LinkedHashSet<>();
    private boolean cleared;

    @Override
    public void addOnClearedListener(RetainedLifecycle.OnClearedListener listener) {
        checkActive();
        listeners.add(listener);
    }

    @Override
    public void removeOnClearedListener(RetainedLifecycle.OnClearedListener listener) {
        checkActive();
        listeners.remove(listener);
    }

    public void dispatchOnCleared() {
        checkActive();
        cleared = true;
        try {
            for (RetainedLifecycle.OnClearedListener listener : listeners) {
                listener.onCleared();
            }
        } finally {
            listeners.clear();
        }
    }

    private void checkActive() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Retained lifecycle callbacks must run on the main thread.");
        }
        if (cleared) {
            throw new IllegalStateException("This retained lifecycle has already been cleared.");
        }
    }
}
