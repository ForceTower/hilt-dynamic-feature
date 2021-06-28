package dev.forcetower.hilt.example.dynamicfeature.injection

import dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature
import dev.forcetower.hilt.example.ValuableDependencies
import dev.forcetower.hilt.example.WorthlessDependencies

@DeclareHiltDynamicFeature(
    dependencies = [
        ValuableDependencies::class,
        WorthlessDependencies::class
    ]
)
abstract class FeatureHilt