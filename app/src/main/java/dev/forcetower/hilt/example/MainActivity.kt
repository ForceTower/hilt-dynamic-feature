package dev.forcetower.hilt.example

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(Button(this).apply {
            text = "Open injected dynamic feature"
            setOnClickListener {
                startActivity(Intent().setClassName(
                    this@MainActivity,
                    "dev.forcetower.hilt.example.dynamicfeature.view.FeatureActivity"
                ))
            }
        })
    }
}
