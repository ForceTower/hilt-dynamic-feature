package dev.forcetower.hilt.example.dynamicfeature.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint
import dev.forcetower.hilt.example.dynamicfeature.injection.FeatureSession
import dev.forcetower.hilt.example.dynamicfeature.injection.FragmentState
import dev.forcetower.hilt.example.dynamicfeature.injection.ScreenState
import javax.inject.Inject

@DynamicAndroidEntryPoint
class FeatureFragment : Fragment() {
    @Inject lateinit var session: FeatureSession
    @Inject lateinit var screenState: ScreenState
    @Inject lateinit var fragmentState: FragmentState
    lateinit var model: FeatureViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[FeatureViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = TextView(requireContext()).apply {
        text = "Fragment injected: ${session.token.elemental}"
    }
}
