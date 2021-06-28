package dev.forcetower.hilt.android.dynamic.managers;

import android.app.Activity;

import androidx.activity.ComponentActivity;

import dagger.hilt.EntryPoint;
import dagger.hilt.EntryPoints;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityRetainedComponent;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.internal.GeneratedComponentManager;

/**
 * Do not use except in Hilt generated code!
 *
 * <p>A manager for the creation of components that live in the Activity.
 *
 * <p>Note: This class is not typed since its type in generated code is always <?> or <Object>. This
 * is mainly due to the fact that we don't know the components at the time of generation, and
 * because even the injector interface type is not a valid type if we have a hilt base class.
 *
 */
public class DynamicActivityComponentManager implements GeneratedComponentManager<Object> {
  /** Entrypoint for {@link ActivityComponentBuilder}. */
  @EntryPoint
  @InstallIn(ActivityRetainedComponent.class)
  public interface ActivityComponentBuilderEntryPoint {
    ActivityComponentBuilder activityComponentBuilder();
  }

  private volatile Object component;
  private final Object componentLock = new Object();

  protected final Activity activity;

  private final GeneratedComponentManager<ActivityRetainedComponent>
      activityRetainedComponentManager;

  public DynamicActivityComponentManager(Activity activity) {
    this.activity = activity;
    this.activityRetainedComponentManager =
        new DynamicActivityRetainedComponentManager((ComponentActivity) activity);
  }

  @Override
  public Object generatedComponent() {
    if (component == null) {
      synchronized (componentLock) {
        if (component == null) {
          component = createComponent();
        }
      }
    }
    return component;
  }

  protected Object createComponent() {
    if (!(activity instanceof DynamicActivityComponentDependant)) {
      throw new IllegalStateException(
              "Hilt Dynamic Activity must be attached to an @DynamicAndroidEntryPoint Application. Found: "
              + activity.getApplication().getClass());
    }

    return EntryPoints.get(
            activityRetainedComponentManager, ActivityComponentBuilderEntryPoint.class)
        .activityComponentBuilder()
        .activity(activity)
        .build();
  }
}
