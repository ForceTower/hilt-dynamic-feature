package dev.forcetower.hilt.android.dynamic.plugin;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.FramesComputationMode;
import com.android.build.api.instrumentation.InstrumentationParameters;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.Variant;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.objectweb.asm.ClassVisitor;

public final class HiltDynamicPlugin implements Plugin<Project> {
    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void apply(Project project) {
        project.getPluginManager().withPlugin("com.android.dynamic-feature", ignored -> {
            AndroidComponentsExtension android =
                    project.getExtensions().getByType(AndroidComponentsExtension.class);
            android.onVariants(android.selector().all(), variantObject -> {
                Variant variant = (Variant) variantObject;
                variant.getJavaCompilation().getAnnotationProcessor().getArguments()
                        .put("dynamic.hilt.enableTransform", "true");
                variant.getInstrumentation().transformClassesWith(
                        Factory.class, InstrumentationScope.PROJECT, params -> kotlin.Unit.INSTANCE);
                variant.getInstrumentation().setAsmFramesComputationMode(
                        FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS);
                return kotlin.Unit.INSTANCE;
            });
        });
        project.afterEvaluate(ignored -> {
            if (!project.getPluginManager().hasPlugin("com.android.dynamic-feature")) {
                throw new GradleException(
                        "dev.forcetower.hilt.dynamic requires com.android.dynamic-feature.");
            }
            if (project.getPluginManager().hasPlugin("com.google.dagger.hilt.android")) {
                throw new GradleException(
                        "Apply the standard Hilt plugin to the base app, and the dynamic Hilt plugin to the feature.");
            }
        });
    }

    public abstract static class Factory
            implements AsmClassVisitorFactory<InstrumentationParameters.None> {
        @Override
        public boolean isInstrumentable(ClassData data) {
            return data.getClassAnnotations().contains(
                    "dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint");
        }

        @Override
        public ClassVisitor createClassVisitor(ClassContext context, ClassVisitor next) {
            return new DynamicEntryPointClassVisitor(next);
        }
    }
}
