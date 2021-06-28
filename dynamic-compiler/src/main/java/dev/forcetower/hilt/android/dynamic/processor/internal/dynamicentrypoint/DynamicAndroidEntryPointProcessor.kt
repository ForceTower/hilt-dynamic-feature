package dev.forcetower.hilt.android.dynamic.processor.internal.dynamicentrypoint

import com.google.auto.service.AutoService
import com.google.common.collect.ImmutableSet
import com.squareup.javapoet.ClassName
import dev.forcetower.hilt.android.dynamic.processor.internal.AndroidClassNames
import dev.forcetower.hilt.android.dynamic.processor.internal.AndroidEntryPointMetadata
import dev.forcetower.hilt.android.dynamic.processor.internal.BaseProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement


@AutoService(Processor::class)
@IncrementalAnnotationProcessor(ISOLATING)
class DynamicAndroidEntryPointProcessor : BaseProcessor() {
    private var dynamicRootComponentName: ClassName? = null
    override fun getSupportedAnnotationTypes(): Set<String> {
        return ImmutableSet.of(
            AndroidClassNames.DYNAMIC_ANDROID_ENTRY_POINT.toString(),
            AndroidClassNames.DECLARE_HILT_DYNAMIC_FEATURE.toString()
        )
    }

    override fun preRoundProcess(roundEnv: RoundEnvironment) {
        super.preRoundProcess(roundEnv)
        dynamicRootComponentName = null
    }

    override fun delayErrors() = true

    override fun processEach(annotation: TypeElement, element: Element) {
        // TODO check strictly for one AND ONLY 1 annotation
        val isRootFeatureNode = element.annotationMirrors.any { mirror ->
            AndroidClassNames.DECLARE_HILT_DYNAMIC_FEATURE.equals(ClassName.get(mirror.annotationType))
        }

        if (isRootFeatureNode) {
            println("Generate root feature node ${element.simpleName}")
            dynamicRootComponentName = RootDynamicFeatureDeclarationGenerator(processingEnv, annotation, element).generate()
        } else {
            val metadata = AndroidEntryPointMetadata.of(processingEnv, element)
            println(metadata)
        }
    }
}