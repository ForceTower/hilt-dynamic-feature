package dev.forcetower.hilt.example.dynamicfeature.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint
import dev.forcetower.hilt.example.dynamicfeature.R

@DynamicAndroidEntryPoint
class FeatureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature)
    }
}