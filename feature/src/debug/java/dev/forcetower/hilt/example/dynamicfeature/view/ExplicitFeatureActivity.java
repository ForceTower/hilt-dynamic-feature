package dev.forcetower.hilt.example.dynamicfeature.view;

import androidx.appcompat.app.AppCompatActivity;
import dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint;
import dev.forcetower.hilt.example.dynamicfeature.injection.FeatureSession;
import javax.inject.Inject;

@DynamicAndroidEntryPoint(AppCompatActivity.class)
public final class ExplicitFeatureActivity extends DynamicHilt_ExplicitFeatureActivity {
    @Inject public FeatureSession session;
}
