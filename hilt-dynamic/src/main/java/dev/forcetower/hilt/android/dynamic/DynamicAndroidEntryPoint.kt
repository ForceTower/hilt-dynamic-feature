package dev.forcetower.hilt.android.dynamic

import dagger.hilt.GeneratesRootInput
import kotlin.reflect.KClass

/**
 * Marks an Android component class to be setup for injection with the standard Hilt Dagger Android
 * components. Currently, this supports activities, fragments, views, services, and broadcast
 * receivers.
 *
 *
 * This annotation will generate a base class that the annotated class should extend, either
 * directly or via the Hilt Gradle Plugin. This base class will take care of injecting members into
 * the Android class as well as handling instantiating the proper Hilt components at the right point
 * in the lifecycle. The name of the base class will be "Hilt_<annotated class name>".
 *
</annotated> *
 * Example usage (with the Hilt Gradle Plugin):
 *
 * <pre>`
 * @AndroidEntryPoint
 * public final class FooActivity extends FragmentActivity {
 * @Inject Foo foo;
 *
 * @Override
 * public void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);  // The foo field is injected in super.onCreate()
 * }
 * }
`</pre> *
 *
 *
 * Example usage (without the Hilt Gradle Plugin):
 *
 * <pre>`
 * @AndroidEntryPoint(FragmentActivity.class)
 * public final class FooActivity extends Hilt_FooActivity {
 * @Inject Foo foo;
 *
 * @Override
 * public void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState);  // The foo field is injected in super.onCreate()
 * }
 * }
`</pre> *
 *
 * @see DeclareHiltDynamicFeature
 */
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@GeneratesRootInput
annotation class DynamicAndroidEntryPoint(
    /**
     * The base class for the generated Hilt class. When applying the Hilt Gradle Plugin this value
     * is not necessary and will be inferred from the current superclass.
     */
    val value: KClass<*> = Void::class
)