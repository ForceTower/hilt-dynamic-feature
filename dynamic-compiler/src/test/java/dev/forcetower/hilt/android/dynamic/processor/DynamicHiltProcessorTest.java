package dev.forcetower.hilt.android.dynamic.processor;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;

public class DynamicHiltProcessorTest {
    private static final JavaFileObject ROOT = JavaFileObjects.forSourceString(
            "dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature", """
            package dev.forcetower.hilt.android.dynamic;
            public @interface DeclareHiltDynamicFeature {
                Class<?>[] dependencies() default {};
            }
            """);
    private static final JavaFileObject ENTRY = JavaFileObjects.forSourceString(
            "dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint", """
            package dev.forcetower.hilt.android.dynamic;
            public @interface DynamicAndroidEntryPoint {
                Class<?> value() default Void.class;
            }
            """);

    @Test
    public void rejectsDependenciesThatAreNotBaseAppEntryPoints() {
        Compilation result = javac().withProcessors(new DynamicHiltProcessor()).compile(ROOT,
                JavaFileObjects.forSourceString("test.Feature", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature(
                    dependencies = {Runnable.class})
                public class Feature {}
                """));
        assertThat(result).failed();
        assertThat(result).hadErrorContaining("public @EntryPoint interfaces installed in SingletonComponent");
    }

    @Test
    public void rejectsSingletonModulesInsideAFeature() {
        Compilation result = javac().withProcessors(new DynamicHiltProcessor()).compile(ROOT,
                JavaFileObjects.forSourceString("test.Feature", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature
                public class Feature {}
                """),
                JavaFileObjects.forSourceString("test.Bindings", """
                package test;
                @dagger.Module
                @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent.class)
                public class Bindings {}
                """));
        assertThat(result).failed();
        assertThat(result).hadErrorContaining("Unsupported feature component");
    }

    @Test
    public void shortFormRequiresTheDynamicGradlePlugin() {
        Compilation result = javac().withProcessors(new DynamicHiltProcessor()).compile(ROOT, ENTRY,
                JavaFileObjects.forSourceString("test.Feature", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature
                public class Feature {}
                """),
                JavaFileObjects.forSourceString("androidx.activity.ComponentActivity", """
                package androidx.activity;
                public class ComponentActivity {}
                """),
                JavaFileObjects.forSourceString("test.Screen", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint
                public class Screen extends androidx.activity.ComponentActivity {}
                """));
        assertThat(result).failed();
        assertThat(result).hadErrorContaining("Apply the dev.forcetower.hilt.dynamic Gradle plugin");
    }

    @Test
    public void rejectsMultipleRoots() {
        Compilation result = javac().withProcessors(new DynamicHiltProcessor()).compile(ROOT,
                JavaFileObjects.forSourceString("test.One", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature
                public class One {}
                """),
                JavaFileObjects.forSourceString("test.Two", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature
                public class Two {}
                """));
        assertThat(result).failed();
        assertThat(result).hadErrorContaining("Only one @DeclareHiltDynamicFeature");
    }

    @Test
    public void rejectsMissingRoot() {
        Compilation result = javac().withProcessors(new DynamicHiltProcessor()).compile(ENTRY,
                JavaFileObjects.forSourceString("test.Screen", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint
                public class Screen {}
                """));
        assertThat(result).failed();
        assertThat(result).hadErrorContaining("Declare exactly one @DeclareHiltDynamicFeature");
    }

    @Test
    public void rejectsUnsupportedAndroidType() {
        Compilation result = javac().withProcessors(new DynamicHiltProcessor()).compile(ROOT, ENTRY,
                JavaFileObjects.forSourceString("test.Feature", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DeclareHiltDynamicFeature
                public class Feature {}
                """),
                JavaFileObjects.forSourceString("test.Screen", """
                package test;
                @dev.forcetower.hilt.android.dynamic.DynamicAndroidEntryPoint
                public class Screen {}
                """));
        assertThat(result).failed();
        assertThat(result).hadErrorContaining("supports ComponentActivity and AndroidX Fragment");
    }
}
