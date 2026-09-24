package dev.forcetower.hilt.example.dynamicfeature.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint
import dev.forcetower.hilt.example.dynamicfeature.R
import dev.forcetower.hilt.example.dynamicfeature.injection.FeatureSession
import dev.forcetower.hilt.example.dynamicfeature.injection.RetainedState
import dev.forcetower.hilt.example.dynamicfeature.injection.ScreenState
import javax.inject.Inject
import javax.inject.Named

@DynamicAndroidEntryPoint
class FeatureActivity : AppCompatActivity() {
    @Inject lateinit var session: FeatureSession
    @Inject lateinit var retainedState: RetainedState
    @Inject lateinit var screenState: ScreenState
    @Inject @field:Named("featureLabel") lateinit var label: String
    lateinit var model: FeatureViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[FeatureViewModel::class.java]
        setContentView(R.layout.activity_feature)
        findViewById<TextView>(R.id.injection_result).text =
            "$label: token ${session.token.elemental}"
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.feature_fragment, FeatureFragment())
                .commitNow()
        }
    }
}
