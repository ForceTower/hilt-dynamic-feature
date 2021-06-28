package dev.forcetower.hilt.android.dynamic.processor.internal.dynamicentrypoint

import com.google.auto.common.MoreElements
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import dagger.hilt.processor.internal.BaseProcessor
import dev.forcetower.hilt.android.dynamic.processor.internal.AndroidClassNames
import dev.forcetower.hilt.android.dynamic.processor.internal.ClassNames
import dev.forcetower.hilt.android.dynamic.processor.internal.ComponentNames
import dev.forcetower.hilt.android.dynamic.processor.internal.Processors
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror

@Suppress("UnstableApiUsage")
class RootDynamicFeatureDeclarationGenerator(
    private val env: ProcessingEnvironment,
    private val annotation: TypeElement,
    private val element: Element
) {
    private val typeElement = MoreElements.asType(this.element)
    private val wrapperClassName = Processors.prepend(Processors.getEnclosedClassName(ClassName.get(typeElement)), "Hilt_")
    private val componentNames = ComponentNames.withoutRenaming()


    // @Generated("ApplicationGenerator")
    // public final class Hilt_$DYNAMIC implements GeneratedComponentManagerHolder {
    //   ...
    // }
    fun generate(): ClassName {
        val typeSpecBuilder = TypeSpec.classBuilder(wrapperClassName.simpleName())
            .addOriginatingElement(element)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(ClassNames.GENERATED_COMPONENT_MANAGER_HOLDER)
            .addMethod(createConstructor())
            .addField(componentManagerField())
            .addField(createLockObjectField())
            .addField(createSingletonField())
            .addMethod(addGetComponentManager())
            .addMethod(addGeneratedComponent())
            .addMethod(addStaticComponentGet())

        Processors.addGeneratedAnnotation(typeSpecBuilder, env, javaClass);

        JavaFile.builder(ClassName.get(typeElement).packageName(), typeSpecBuilder.build())
            .build()
            .writeTo(env.filer)

        return wrapperClassName
    }

    /**
     * public static Object getGeneratedComponent() {
     *     if (INSTANCE == null) {
     *          synchronized(LOCK) {
     *              if (INSTANCE == null) {
     *                  INSTANCE = new Hilt_$DYNAMIC(context);
     *              }
     *          }
     *     }
     *     return INSTANCE;
     * }
     */
    private fun addStaticComponentGet(): MethodSpec {
        return MethodSpec.methodBuilder("getGeneratedComponent")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.OBJECT)
            .addParameter(AndroidClassNames.CONTEXT, "context")
            .beginControlFlow("if (INSTANCE == null)")
            .beginControlFlow("synchronized(LOCK)")
            .beginControlFlow("if (INSTANCE == null)")
            .addStatement("INSTANCE = new ${"$"}T(context)", wrapperClassName)
            .endControlFlow()
            .endControlFlow()
            .endControlFlow()
            .addStatement("return INSTANCE")
            .build()
    }

    // private static Hilt_$DYNAMIC INSTANCE = null;
    private fun createSingletonField(): FieldSpec {
        return FieldSpec.builder(wrapperClassName, "INSTANCE", Modifier.PRIVATE, Modifier.STATIC)
            .initializer("null")
            .build()
    }

    // private static final Object LOCK = new Object();
    private fun createLockObjectField(): FieldSpec {
        return FieldSpec.builder(TypeName.OBJECT, "LOCK", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer(CodeBlock.of("new Object()"))
            .build()
    }

    private fun addGetComponentManager(): MethodSpec {
        return MethodSpec.methodBuilder("componentManager")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .returns(ParameterizedTypeName.get(ClassNames.GENERATED_COMPONENT_MANAGER, TypeName.OBJECT))
            .addStatement(CodeBlock.of(
                "return componentManager"
            ))
            .build()
    }

    private fun componentManagerField(): FieldSpec {
        return FieldSpec.builder(AndroidClassNames.DYNAMIC_FEATURE_COMPONENT_MANAGER, "componentManager")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build()
    }

    private fun createConstructor(): MethodSpec {
        return MethodSpec.constructorBuilder()
            .addParameter(AndroidClassNames.CONTEXT, "context")
            .addStatement(createRootComponent())
            .build()
    }

    private fun createRootComponent(): CodeBlock {
        return CodeBlock.of(
            "componentManager = new \$T(\$L)",
            AndroidClassNames.DYNAMIC_FEATURE_COMPONENT_MANAGER,
            createComponentManager()
        )
    }

    private fun createComponentManager(): TypeSpec {
        val component = componentNames.generatedComponent(
            ClassName.get(typeElement), AndroidClassNames.DYNAMIC_FEATURE_COMPONENT
        )
        return TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(AndroidClassNames.COMPONENT_SUPPLIER)
            .addMethod(
                MethodSpec.methodBuilder("get")
                    .addAnnotation(Override::class.java)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.OBJECT)
                    .addStatement(
                        """
                            return ${"$"}T.builder()
                            .applicationContextModule(new ${"$"}T(context.getApplicationContext()))
                            ${"$"}L
                            .build()
                        """.trimIndent(),
                        Processors.prepend(Processors.getEnclosedClassName(component), "Dagger"),
                        AndroidClassNames.APPLICATION_CONTEXT_MODULE,
                        unrollComponentDependencies()
                    )
                    .build()
            )
            .build()
    }

    private fun unrollComponentDependencies(): CodeBlock {
        val dependenciesDeclaration = element.annotationMirrors.first { mirror ->
            AndroidClassNames.DECLARE_HILT_DYNAMIC_FEATURE.equals(ClassName.get(mirror.annotationType))
        }
        val blocks = dependenciesDeclaration.elementValues.entries.flatMap { entry ->
            if ("dependencies" == entry.key.simpleName.toString()) {
                val other = entry.value.value as List<*>
                other.map { annotation ->
                    val type = (annotation as AnnotationValue).value as TypeMirror
                    val camel = type.toString().split(".").last().replaceFirstChar { if (it.isUpperCase()) it.lowercase().toCharArray()[0] else it }
                    CodeBlock.of(
                        """
                            .${camel}(${"$"}L)
                        """.trimIndent(),
                        buildEntryPointFor(type)
                    )
                }
            } else {
                emptyList()
            }
        }
        return CodeBlock.join(blocks, "\n")
    }

    private fun buildEntryPointFor(type: TypeMirror): CodeBlock {
        return CodeBlock.of(
            """
                ${"$"}T.fromApplication(
                    context, 
                    ${"$"}T.class
                )
            """.trimIndent(),
            AndroidClassNames.ENTRY_POINT_ACCESSORS,
            type
        )
    }

    private fun addGeneratedComponent(): MethodSpec {
        return MethodSpec.methodBuilder("generatedComponent")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .returns(TypeName.OBJECT)
            .addStatement(
                "return \$L.generatedComponent()",
                componentManagerCallBlock()
            )
            .build()
    }

    private fun componentManagerCallBlock(): CodeBlock? {
        return CodeBlock.of(
            "\$L.componentManager()",
            "this"
        )
    }

    fun unsafeCastThisTo(castType: ClassName): CodeBlock {
        return CodeBlock.of("\$T.<\$T>unsafeCast(this)", ClassNames.UNSAFE_CASTS, castType)
    }
}