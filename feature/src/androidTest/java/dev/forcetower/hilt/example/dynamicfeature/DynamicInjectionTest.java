package dev.forcetower.hilt.example.dynamicfeature;

import static org.junit.Assert.*;

import android.widget.TextView;
import android.content.Intent;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.EntryPoints;
import dev.forcetower.hilt.example.dynamicfeature.injection.FeatureAccess;
import dev.forcetower.hilt.example.dynamicfeature.injection.ScreenAccess;
import dev.forcetower.hilt.example.dynamicfeature.injection.Hilt_FeatureHilt;
import dev.forcetower.hilt.example.MainActivity;
import dev.forcetower.hilt.example.ValuableDependencies;
import dev.forcetower.hilt.example.dynamicfeature.view.FeatureActivity;
import dev.forcetower.hilt.example.dynamicfeature.view.FeatureFragment;
import dev.forcetower.hilt.example.dynamicfeature.view.FeatureViewModel;
import dev.forcetower.hilt.example.dynamicfeature.view.ExplicitFeatureActivity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DynamicInjectionTest {
    @Test
    public void activityAndFragmentReceiveBaseAppAndFeatureBindings() {
        try (ActivityScenario<FeatureActivity> scenario = ActivityScenario.launch(FeatureActivity.class)) {
            scenario.onActivity(activity -> {
                assertSame(EntryPointAccessors.fromApplication(activity, ValuableDependencies.class).database(),
                        activity.getSession().getToken());
                assertSame(activity.getApplication(), activity.getSession().getContext());
                assertSame(activity, activity.getScreenState().getContext());
                assertSame(activity.getSession(), EntryPoints.get(
                        Hilt_FeatureHilt.getGeneratedComponent(activity), FeatureAccess.class).session());
                assertSame(activity.getScreenState(), EntryPoints.get(activity, ScreenAccess.class).screen());
                assertEquals("Hilt Dynamic feature injected: token 123456",
                        ((TextView) activity.findViewById(R.id.injection_result)).getText().toString());
                FeatureFragment fragment = (FeatureFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.feature_fragment);
                assertNotNull(fragment);
                assertSame(activity.getSession(), fragment.getSession());
                assertSame(activity.getScreenState(), fragment.getScreenState());
                assertNotNull(fragment.getFragmentState());
                assertSame(activity.getRetainedState(), fragment.getModel().getRetainedState());
                assertNotSame(activity.getModel(), fragment.getModel());
            });
        }
    }

    @Test
    public void explicitGeneratedSuperclassIsSupported() {
        try (ActivityScenario<ExplicitFeatureActivity> scenario =
                     ActivityScenario.launch(ExplicitFeatureActivity.class)) {
            scenario.onActivity(activity -> assertSame(
                    EntryPointAccessors.fromApplication(activity, ValuableDependencies.class).database(),
                    activity.session.getToken()));
        }
    }

    @Test
    public void recreationRetainsOnlyFeatureAndRetainedScopes() {
        try (ActivityScenario<FeatureActivity> scenario = ActivityScenario.launch(FeatureActivity.class)) {
            FeatureActivity[] original = new FeatureActivity[1];
            scenario.onActivity(activity -> original[0] = activity);
            scenario.recreate();
            scenario.onActivity(recreated -> {
                assertNotSame(original[0], recreated);
                assertSame(original[0].getSession(), recreated.getSession());
                assertSame(original[0].getRetainedState(), recreated.getRetainedState());
                assertFalse(recreated.getRetainedState().getCleared());
                assertNotSame(original[0].getScreenState(), recreated.getScreenState());
                assertSame(recreated, recreated.getScreenState().getContext());
            });
        }
    }

    @Test
    public void separateActivitiesShareOnlyFeatureScope() {
        FeatureActivity[] original = new FeatureActivity[1];
        try (ActivityScenario<FeatureActivity> scenario = ActivityScenario.launch(FeatureActivity.class)) {
            scenario.onActivity(activity -> original[0] = activity);
        }
        assertTrue(original[0].getRetainedState().getCleared());
        try (ActivityScenario<FeatureActivity> scenario = ActivityScenario.launch(FeatureActivity.class)) {
            scenario.onActivity(activity -> {
                assertSame(original[0].getSession(), activity.getSession());
                assertNotSame(original[0].getRetainedState(), activity.getRetainedState());
                assertNotSame(original[0].getScreenState(), activity.getScreenState());
            });
        }
    }

    @Test
    public void hiltViewModelsReceiveSavedStateAndRespectTheirLifecycle() {
        FeatureViewModel[] original = new FeatureViewModel[1];
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), FeatureActivity.class)
                .putExtra("route", "from-intent");
        try (ActivityScenario<FeatureActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                FeatureViewModel model = activity.getModel();
                original[0] = model;
                assertSame(activity.getSession(), model.getSession());
                assertSame(activity.getRetainedState(), model.getRetainedState());
                assertSame(model.getFirst(), model.getSecond());
                assertEquals("ViewModel injected", model.getLabel());
                assertEquals("from-intent", model.getState().get("route"));
                model.getState().set("count", 42);
                FeatureViewModel another = new ViewModelProvider(activity)
                        .get("another", FeatureViewModel.class);
                assertNotSame(model, another);
                assertNotSame(model.getFirst(), another.getFirst());
                assertSame(model.getRetainedState(), another.getRetainedState());
                assertNotNull(new ViewModelProvider(activity).get(PlainViewModel.class));
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertSame(original[0], activity.getModel());
                assertEquals(Integer.valueOf(42), activity.getModel().getState().get("count"));
                assertFalse(activity.getModel().getFirst().getCleared());
            });
        }
        assertTrue(original[0].getFirst().getCleared());
        assertTrue(original[0].getRetainedState().getCleared());
    }

    public static class PlainViewModel extends ViewModel {}

    @Test
    public void featureFragmentsWorkInTheBaseAppsHiltActivity() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                FeatureFragment first = new FeatureFragment();
                FeatureFragment second = new FeatureFragment();
                activity.getSupportFragmentManager().beginTransaction()
                        .add(android.R.id.content, first, "first")
                        .add(android.R.id.content, second, "second").commitNow();
                assertSame(first.getSession(), second.getSession());
                assertSame(first.getScreenState(), second.getScreenState());
                assertNotSame(first.getFragmentState(), second.getFragmentState());
                assertSame(activity, first.getScreenState().getContext());
            });
            FeatureFragment[] original = new FeatureFragment[1];
            scenario.onActivity(activity -> original[0] = (FeatureFragment) activity
                    .getSupportFragmentManager().findFragmentByTag("first"));
            scenario.recreate();
            scenario.onActivity(activity -> {
                FeatureFragment recreated = (FeatureFragment) activity
                        .getSupportFragmentManager().findFragmentByTag("first");
                assertNotSame(original[0], recreated);
                assertSame(original[0].getSession(), recreated.getSession());
                assertNotSame(original[0].getScreenState(), recreated.getScreenState());
            });
        }
    }
}
