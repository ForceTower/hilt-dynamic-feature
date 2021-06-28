package dev.forcetower.hilt.android.dynamic.processor.internal.dynamicentrypoint

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import dev.forcetower.hilt.android.dynamic.processor.internal.AndroidClassNames
import dev.forcetower.hilt.android.dynamic.processor.internal.AndroidEntryPointMetadata
import dev.forcetower.hilt.android.dynamic.processor.internal.ClassNames
import dev.forcetower.hilt.android.dynamic.processor.internal.Processors
import dev.forcetower.hilt.android.dynamic.processor.internal.dynamicentrypoint.Generators.unsafeCastThisTo
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Modifier


class DynamicActivityGenerator(
    private val env: ProcessingEnvironment,
    private val metadata: AndroidEntryPointMetadata,
    private val rootComponentName: ClassName?
) {
    private val generatedClassName = metadata.generatedClassName();

    fun generate() {
        val builder = TypeSpec.classBuilder(generatedClassName.simpleName())
            .addOriginatingElement(metadata.element())
            .superclass(metadata.baseClassName())
            .addSuperinterface(ClassNames.GENERATED_COMPONENT_MANAGER_HOLDER)
            .addSuperinterface(ClassNames.DYNAMIC_ACTIVITY_COMPONENT_DEPENDANT)
            .addModifiers(*metadata.generatedClassModifiers())
            .addField(addComponentField())
            .addField(addComponentLockField())
            .addField(addInjectedField())

        Generators.addGeneratedBaseClassJavadoc(builder, AndroidClassNames.DYNAMIC_ANDROID_ENTRY_POINT)
        Processors.addGeneratedAnnotation(builder, env, javaClass)

        Generators.copyConstructors(
            metadata.baseElement(),
            CodeBlock.builder().addStatement("_initHiltInternal()").build(),
            builder
        )

        builder.addMethod(init())

        metadata.baseElement().typeParameters.map {
            TypeVariableName.get(it)
        }.forEach {
            builder.addTypeVariable(it)
        }

        Generators.copyLintAnnotations(metadata.element(), builder)

        builder.addMethod(createDynamicActivityComponentManager())
        builder.addMethod(getDynamicActivityComponentManager())
        builder.addMethod(getDynamicComponent())
        builder.addMethod(createInject())


        if (Processors.isAssignableFrom(metadata.baseElement(), AndroidClassNames.COMPONENT_ACTIVITY)
            && !metadata.overridesAndroidEntryPointClass()) {
            builder.addMethod(getDefaultViewModelProviderFactory());
        }

        JavaFile.builder(generatedClassName.packageName(), builder.build())
            .build()
            .writeTo(env.filer)
    }

    private fun createInject(): MethodSpec {
        return MethodSpec.methodBuilder("inject")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .beginControlFlow("if (!injected)")
            .addStatement("injected = true")
            .addStatement(
                "((\$T) \$L).\$L(\$L)",
                metadata.injectorClassName(),
                Generators.generatedComponentCallBlock(metadata),
                metadata.injectMethodName(),
                unsafeCastThisTo(metadata.elementClassName()))
            .endControlFlow()
            .build()
    }

    // @Override
    // public final Object getDynamicComponent() {
    //     return Hilt_AuthHiltDynamicComponent.getComponentInstance(this);
    // }
    private fun getDynamicComponent(): MethodSpec {
        return MethodSpec.methodBuilder("getDynamicComponent")
            .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
            .returns(TypeName.OBJECT)
            .addStatement(CodeBlock.of(
                """
                    return ${"$"}T.getGeneratedComponent(this)
                """.trimIndent(),
                rootComponentName
            ))
            .build()
    }

    private fun addInjectedField(): FieldSpec {
        return FieldSpec.builder(ClassName.BOOLEAN, "injected")
            .addModifiers(Modifier.PRIVATE)
            .initializer(CodeBlock.of("false"))
            .build()
    }

    private fun addComponentLockField(): FieldSpec {
        return FieldSpec.builder(ClassName.OBJECT, "componentManagerLock")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer(CodeBlock.of("new Object()"))
            .build()
    }

    private fun addComponentField(): FieldSpec {
        return FieldSpec.builder(AndroidClassNames.DYNAMIC_ACTIVITY_COMPONENT_MANAGER, "componentManager")
            .addModifiers(Modifier.PRIVATE, Modifier.VOLATILE)
            .build()
    }

    private fun createDynamicActivityComponentManager(): MethodSpec {
        return MethodSpec.methodBuilder("createComponentManager")
            .addModifiers(Modifier.PROTECTED)
            .returns(AndroidClassNames.DYNAMIC_ACTIVITY_COMPONENT_MANAGER)
            .addStatement(CodeBlock.of(
                "return new \$T(this)",
                AndroidClassNames.DYNAMIC_ACTIVITY_COMPONENT_MANAGER
            ))
            .build()
    }

    private fun getDynamicActivityComponentManager(): MethodSpec {
        return MethodSpec.methodBuilder("componentManager")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .returns(AndroidClassNames.DYNAMIC_ACTIVITY_COMPONENT_MANAGER)
            .addAnnotation(Override::class.java)
            .beginControlFlow("if (componentManager == null)")
            .beginControlFlow("synchronized (componentManagerLock)")
            .beginControlFlow("if (componentManager == null)")
            .addStatement("componentManager = createComponentManager()")
            .endControlFlow()
            .endControlFlow()
            .endControlFlow()
            .addStatement("return componentManager")
            .build()
    }

    private fun init(): MethodSpec {
        return MethodSpec.methodBuilder("_initHiltInternal")
            .addModifiers(Modifier.PRIVATE)
            .addStatement(
                "addOnContextAvailableListener(\$L)",
                TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(AndroidClassNames.ON_CONTEXT_AVAILABLE_LISTENER)
                    .addMethod(
                        MethodSpec.methodBuilder("onContextAvailable")
                            .addAnnotation(Override::class.java)
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(AndroidClassNames.CONTEXT, "context")
                            .addStatement("inject()")
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun getDefaultViewModelProviderFactory(): MethodSpec {
        return MethodSpec.methodBuilder("getDefaultViewModelProviderFactory")
            .addAnnotation(Override::class.java)
            .addModifiers(Modifier.PUBLIC)
            .returns(AndroidClassNames.VIEW_MODEL_PROVIDER_FACTORY)
            .addStatement(
                "return \$T.getActivityFactory(this, super.getDefaultViewModelProviderFactory())",
                AndroidClassNames.DEFAULT_VIEW_MODEL_FACTORIES
            )
            .build()
    }
}
