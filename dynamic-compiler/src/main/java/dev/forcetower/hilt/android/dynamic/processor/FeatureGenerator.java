package dev.forcetower.hilt.android.dynamic.processor;

import static dev.forcetower.hilt.android.dynamic.processor.DynamicHiltProcessor.*;
import static javax.lang.model.element.Modifier.*;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

final class FeatureGenerator {
    private final DynamicHiltProcessor processor;
    private final ProcessingEnvironment env;
    private final TypeElement root;
    private final ClassName wrapper;
    private final ClassName graph;
    private final Map<String, List<TypeName>> modules = new LinkedHashMap<>();
    private final Map<String, List<TypeName>> entryPoints = new LinkedHashMap<>();

    FeatureGenerator(DynamicHiltProcessor processor, ProcessingEnvironment env, TypeElement root) {
        this.processor = processor;
        this.env = env;
        this.root = root;
        wrapper = ClassName.get(processor.packageName(root), "Hilt_" + processor.flattened(root));
        graph = ClassName.get(processor.packageName(root), processor.flattened(root) + "_Components");
        for (String component : List.of(FEATURE, RETAINED, ACTIVITY, FRAGMENT, VIEWMODEL)) {
            modules.put(component, new ArrayList<>());
            entryPoints.put(component, new ArrayList<>());
        }
    }

    private static ClassName name(String qualifiedName) {
        return ClassName.bestGuess(qualifiedName);
    }

    private static ClassName component(String simpleName) {
        return name(API + "components." + simpleName);
    }

    private ClassName baseName(TypeElement entry) {
        return ClassName.get(processor.packageName(entry), "DynamicHilt_" + processor.flattened(entry));
    }

    private ClassName injectorName(TypeElement entry) {
        return ClassName.get(processor.packageName(entry), processor.flattened(entry) + "_GeneratedInjector");
    }

    private TypeElement base(TypeElement entry) {
        TypeMirror explicit = (TypeMirror) processor.value(processor.annotation(entry, ENTRY), "value").getValue();
        TypeMirror type = explicit.toString().equals("java.lang.Void") ? entry.getSuperclass() : explicit;
        return (TypeElement) env.getTypeUtils().asElement(type);
    }

    private boolean isActivity(TypeElement base) {
        TypeElement activity = env.getElementUtils().getTypeElement("androidx.activity.ComponentActivity");
        return activity != null && env.getTypeUtils().isAssignable(base.asType(), activity.asType());
    }

    private boolean isFragment(TypeElement base) {
        TypeElement fragment = env.getElementUtils().getTypeElement("androidx.fragment.app.Fragment");
        return fragment != null && env.getTypeUtils().isAssignable(base.asType(), fragment.asType());
    }

    void generateBase(TypeElement entry) throws IOException {
        if (!processor.validEntry(entry)) {
            return;
        }
        TypeElement parent = base(entry);
        if (parent == null || (!isActivity(parent) && !isFragment(parent))) {
            processor.error(entry, "@DynamicAndroidEntryPoint supports ComponentActivity and AndroidX Fragment.");
            return;
        }
        if (!parent.getTypeParameters().isEmpty() || parent.getModifiers().contains(FINAL)) {
            processor.error(entry, "The Android base class must be non-final and non-generic.");
            return;
        }
        for (TypeElement ancestor = parent; ancestor != null;
                ancestor = (TypeElement) env.getTypeUtils().asElement(ancestor.getSuperclass())) {
            if (processor.annotation(ancestor, ENTRY) != null
                    || processor.annotation(ancestor, "dagger.hilt.android.AndroidEntryPoint") != null) {
                processor.error(entry, "An entry point cannot extend another Hilt entry point.");
                return;
            }
        }
        TypeMirror explicit = (TypeMirror) processor.value(processor.annotation(entry, ENTRY), "value").getValue();
        if (explicit.toString().equals("java.lang.Void")
                && !Boolean.parseBoolean(env.getOptions().get("dynamic.hilt.enableTransform"))) {
            processor.error(entry, "Apply the dev.forcetower.hilt.dynamic Gradle plugin, or specify "
                    + "@DynamicAndroidEntryPoint(Base.class) and extend " + baseName(entry) + ".");
            return;
        }
        if (!explicit.toString().equals("java.lang.Void")
                && !entry.getSuperclass().toString().equals(baseName(entry).toString())
                && !entry.getSuperclass().toString().equals(baseName(entry).simpleName())) {
            processor.error(entry, "With an explicit base class, extend " + baseName(entry) + ".");
            return;
        }

        boolean activity = isActivity(parent);
        ClassName manager = name(API + "managers.Dynamic"
                + (activity ? "Activity" : "Fragment") + "ComponentManager");
        ClassName generated = baseName(entry);
        TypeSpec.Builder type = TypeSpec.classBuilder(generated)
                .addModifiers(PUBLIC, ABSTRACT).superclass(TypeName.get(parent.asType()))
                .addOriginatingElement(entry)
                .addSuperinterface(name("dagger.hilt.internal.GeneratedComponentManagerHolder"))
                .addField(manager, "componentManager", PRIVATE, VOLATILE)
                .addField(FieldSpec.builder(Object.class, "componentLock", PRIVATE, FINAL)
                        .initializer("new Object()").build())
                .addField(boolean.class, "injected", PRIVATE);
        int constructors = 0;
        for (ExecutableElement constructor : ElementFilter.constructorsIn(parent.getEnclosedElements())) {
            if (constructor.getModifiers().contains(PRIVATE)
                    || (!constructor.getModifiers().contains(PUBLIC)
                    && !constructor.getModifiers().contains(PROTECTED)
                    && !processor.packageName(parent).equals(processor.packageName(entry)))) {
                continue;
            }
            MethodSpec.Builder copy = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
            List<CodeBlock> arguments = new ArrayList<>();
            constructor.getParameters().forEach(parameter -> {
                String parameterName = parameter.getSimpleName().toString();
                copy.addParameter(TypeName.get(parameter.asType()), parameterName);
                arguments.add(CodeBlock.of("$L", parameterName));
            });
            constructor.getThrownTypes().forEach(thrown -> copy.addException(TypeName.get(thrown)));
            copy.varargs(constructor.isVarArgs());
            copy.addStatement("super($L)", CodeBlock.join(arguments, ", "));
            if (activity) {
                copy.addStatement("addOnContextAvailableListener(context -> inject())");
            }
            type.addMethod(copy.build());
            constructors++;
        }
        if (constructors == 0) {
            processor.error(entry, "The Android base class needs an accessible constructor.");
            return;
        }
        CodeBlock managerCreation = activity
                ? CodeBlock.of("new $T(this, $T.getGeneratedComponent(this))", manager, wrapper)
                : CodeBlock.of("new $T(this, $T.getGeneratedComponent(requireContext()))", manager, wrapper);
        type.addMethod(MethodSpec.methodBuilder("componentManager").addAnnotation(Override.class)
                .addModifiers(PUBLIC, FINAL).returns(manager)
                .beginControlFlow("if (componentManager == null)")
                .beginControlFlow("synchronized (componentLock)")
                .beginControlFlow("if (componentManager == null)")
                .addStatement("componentManager = $L", managerCreation)
                .endControlFlow().endControlFlow().endControlFlow()
                .addStatement("return componentManager").build());
        type.addMethod(MethodSpec.methodBuilder("generatedComponent").addAnnotation(Override.class)
                .addModifiers(PUBLIC, FINAL).returns(Object.class)
                .addStatement("return componentManager().generatedComponent()").build());
        type.addMethod(MethodSpec.methodBuilder("getDefaultViewModelProviderFactory")
                .addAnnotation(Override.class).addModifiers(PUBLIC)
                .returns(name("androidx.lifecycle.ViewModelProvider.Factory"))
                .addStatement("return $T.viewModelFactory(componentManager().generatedComponent()"
                                + ".viewModelComponentBuilder(), super.getDefaultViewModelProviderFactory())", wrapper)
                .build());
        type.addMethod(MethodSpec.methodBuilder("inject").addModifiers(PRIVATE)
                .beginControlFlow("if (!injected)")
                .addStatement("(($T) generatedComponent()).inject$L(($T) (Object) this)",
                        injectorName(entry), processor.flattened(entry), ClassName.get(entry))
                .addStatement("injected = true").endControlFlow().build());
        if (!activity) {
            type.addField(name("android.app.Activity"), "injectedActivity", PRIVATE);
            type.addMethod(MethodSpec.methodBuilder("onAttach").addAnnotation(Override.class)
                    .addModifiers(PUBLIC).addParameter(name("android.content.Context"), "context")
                    .addStatement("super.onAttach(context)")
                    .beginControlFlow("if (injectedActivity != null && injectedActivity != requireActivity())")
                    .addStatement("throw new $T($S)", IllegalStateException.class,
                            "Dynamic Hilt fragments cannot be retained across activities.")
                    .endControlFlow().addStatement("injectedActivity = requireActivity()")
                    .addStatement("inject()").build());
        }
        write(generated.packageName(), type.build());
        write(injectorName(entry).packageName(), TypeSpec.interfaceBuilder(injectorName(entry))
                .addOriginatingElement(entry).addModifiers(PUBLIC)
                .addMethod(MethodSpec.methodBuilder("inject" + processor.flattened(entry))
                        .addModifiers(PUBLIC, ABSTRACT).addParameter(ClassName.get(entry), "instance").build())
                .build());
    }

    void generateGraph(List<TypeElement> entries, List<TypeElement> installed,
            List<TypeElement> viewModels) throws IOException {
        if (!root.getTypeParameters().isEmpty()) {
            processor.error(root, "A dynamic feature declaration cannot have type parameters.");
        }
        List<TypeMirror> dependencies = processor.types(processor.annotation(root, ROOT), "dependencies");
        for (TypeMirror dependency : dependencies) {
            TypeElement type = (TypeElement) env.getTypeUtils().asElement(dependency);
            AnnotationMirror install = processor.annotation(type, INSTALL);
            if (type.getKind() != ElementKind.INTERFACE || !processor.publiclyAccessible(type)
                    || processor.annotation(type, "dagger.hilt.EntryPoint") == null
                    || install == null
                    || !processor.types(install, "value").stream().map(Object::toString)
                            .toList().equals(List.of(SINGLETON))) {
                processor.error(root, "Feature dependencies must be public @EntryPoint interfaces "
                        + "installed in SingletonComponent: " + dependency);
            }
        }
        for (TypeElement type : installed) {
            boolean module = processor.annotation(type, "dagger.Module") != null;
            boolean entryPoint = processor.annotation(type, "dagger.hilt.EntryPoint") != null;
            if (!module && !entryPoint) {
                processor.error(type, "@InstallIn requires @Module or @EntryPoint.");
                continue;
            }
            if (!processor.publiclyAccessible(type)) {
                processor.error(type, "Feature modules and entry point interfaces must be public.");
            }
            for (TypeMirror target : processor.types(processor.annotation(type, INSTALL), "value")) {
                String key = target.toString();
                if (!modules.containsKey(key)) {
                    processor.error(type, "Unsupported feature component: " + key
                            + ". Use DynamicFeatureComponent, ActivityRetainedComponent, "
                            + "ActivityComponent, FragmentComponent or ViewModelComponent. "
                            + "Base app bindings belong in the base app.");
                } else {
                    (module ? modules : entryPoints).get(key).add(TypeName.get(type.asType()));
                }
            }
        }
        for (TypeElement model : viewModels) {
            TypeElement viewModel = env.getElementUtils().getTypeElement("androidx.lifecycle.ViewModel");
            long constructors = ElementFilter.constructorsIn(model.getEnclosedElements()).stream()
                    .filter(it -> processor.annotation(it, "javax.inject.Inject") != null).count();
            if (!processor.publiclyAccessible(model) || !model.getTypeParameters().isEmpty()
                    || model.getModifiers().contains(ABSTRACT)
                    || viewModel == null || !env.getTypeUtils().isAssignable(model.asType(), viewModel.asType())
                    || constructors != 1) {
                processor.error(model, "@HiltViewModel requires a public, non-generic ViewModel "
                        + "with exactly one @Inject constructor.");
            }
            if (!processor.value(processor.annotation(model, HILT_VIEWMODEL), "assistedFactory")
                    .getValue().toString().equals("java.lang.Object")) {
                processor.error(model, "Assisted @HiltViewModel factories are not supported in dynamic features.");
            }
        }
        if (processor.failed()) {
            return;
        }
        ClassName feature = graph.nestedClass("FeatureC");
        ClassName retained = graph.nestedClass("RetainedC");
        ClassName activity = graph.nestedClass("ActivityC");
        ClassName fragment = graph.nestedClass("FragmentC");
        ClassName viewModel = graph.nestedClass("ViewModelC");
        modules.get(ACTIVITY).add(graph.nestedClass("ActivityBindings"));
        modules.get(VIEWMODEL).add(graph.nestedClass("ViewModelBindings"));
        modules.get(RETAINED).add(graph.nestedClass("RetainedBindings"));
        TypeSpec.Builder outer = TypeSpec.classBuilder(graph).addModifiers(PUBLIC, FINAL)
                .addOriginatingElement(root).addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build());
        entries.forEach(outer::addOriginatingElement);
        installed.forEach(outer::addOriginatingElement);
        viewModels.forEach(outer::addOriginatingElement);
        outer.addType(viewModelBindings(viewModels));
        outer.addType(TypeSpec.classBuilder("RetainedBindings").addModifiers(PUBLIC, STATIC, ABSTRACT)
                .addAnnotation(name("dagger.Module"))
                .addMethod(MethodSpec.methodBuilder("viewModelBuilder").addModifiers(PUBLIC, ABSTRACT)
                        .addAnnotation(name("dagger.Binds"))
                        .addParameter(viewModel.nestedClass("Builder"), "builder")
                        .returns(component("DynamicViewModelComponent").nestedClass("Builder")).build())
                .build());
        outer.addType(TypeSpec.classBuilder("ActivityBindings").addModifiers(PUBLIC, STATIC, FINAL)
                .addAnnotation(name("dagger.Module"))
                .addMethod(MethodSpec.methodBuilder("context").addModifiers(PUBLIC, STATIC)
                        .addAnnotation(name("dagger.Provides"))
                        .addAnnotation(name("dagger.hilt.android.qualifiers.ActivityContext"))
                        .returns(name("android.content.Context"))
                        .addParameter(name("android.app.Activity"), "activity")
                        .addStatement("return activity").build()).build());

        TypeSpec.Builder featureType = graphType(feature, FEATURE, "dagger.Component", API + "scopes.DynamicScope")
                .addSuperinterface(component("DynamicFeatureComponent"))
                .addMethod(builderGetter("retainedComponentBuilder", retained))
                .addType(rootFactory(feature, dependencies));
        AnnotationSpec.Builder featureAnnotation = AnnotationSpec.builder(name("dagger.Component"))
                .addMember("modules", "$L", classArray(modules.get(FEATURE)))
                .addMember("dependencies", "$L", classArray(dependencies.stream().map(TypeName::get).toList()));
        featureType.addAnnotation(featureAnnotation.build());
        outer.addType(featureType.build());

        outer.addType(graphType(retained, RETAINED, "dagger.Subcomponent",
                        "dagger.hilt.android.scopes.ActivityRetainedScoped")
                .addSuperinterface(component("DynamicActivityRetainedComponent"))
                .addMethod(builderGetter("activityComponentBuilder", activity))
                .addMethod(builderGetter("viewModelComponentBuilder", viewModel))
                .addType(builder(retained, component("DynamicActivityRetainedComponent"),
                        name("dagger.hilt.android.ActivityRetainedLifecycle"), "lifecycle"))
                .build());
        TypeSpec.Builder activityType = graphType(activity, ACTIVITY, "dagger.Subcomponent",
                        "dagger.hilt.android.scopes.ActivityScoped")
                .addSuperinterface(component("DynamicActivityComponent"))
                .addMethod(builderGetter("fragmentComponentBuilder", fragment))
                .addType(builder(activity, component("DynamicActivityComponent"),
                        name("android.app.Activity"), "activity"));
        TypeSpec.Builder fragmentType = graphType(fragment, FRAGMENT, "dagger.Subcomponent",
                        "dagger.hilt.android.scopes.FragmentScoped")
                .addSuperinterface(component("DynamicFragmentComponent"))
                .addType(builder(fragment, component("DynamicFragmentComponent"),
                        name("androidx.fragment.app.Fragment"), "fragment"));
        for (TypeElement entry : entries) {
            (isActivity(base(entry)) ? activityType : fragmentType).addSuperinterface(injectorName(entry));
        }
        outer.addType(activityType.build()).addType(fragmentType.build());
        outer.addType(graphType(viewModel, VIEWMODEL, "dagger.Subcomponent",
                        "dagger.hilt.android.scopes.ViewModelScoped")
                .addSuperinterface(component("DynamicViewModelComponent"))
                .addType(builder(viewModel, component("DynamicViewModelComponent"),
                        name("androidx.lifecycle.SavedStateHandle"), "savedStateHandle").toBuilder()
                        .addMethod(MethodSpec.methodBuilder("lifecycle")
                                .addModifiers(PUBLIC, ABSTRACT).addAnnotation(Override.class)
                                .addAnnotation(name("dagger.BindsInstance"))
                                .addParameter(name("dagger.hilt.android.ViewModelLifecycle"), "lifecycle")
                                .returns(viewModel.nestedClass("Builder")).build()).build()).build());
        write(graph.packageName(), outer.build());
        generateWrapper(dependencies, viewModels);
    }

    private TypeSpec viewModelBindings(List<TypeElement> models) {
        TypeName map = ParameterizedTypeName.get(ClassName.get(Map.class),
                ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(Object.class)),
                name("androidx.lifecycle.ViewModel"));
        TypeSpec.Builder module = TypeSpec.classBuilder("ViewModelBindings")
                .addModifiers(PUBLIC, STATIC, ABSTRACT).addAnnotation(name("dagger.Module"))
                .addMethod(MethodSpec.methodBuilder("viewModels").addModifiers(PUBLIC, ABSTRACT)
                        .addAnnotation(name("dagger.multibindings.Multibinds"))
                        .addAnnotation(name(API + "lifecycle.DynamicViewModelMap")).returns(map).build());
        for (int i = 0; i < models.size(); i++) {
            TypeElement model = models.get(i);
            module.addMethod(MethodSpec.methodBuilder("bindViewModel" + i).addModifiers(PUBLIC, ABSTRACT)
                    .addAnnotation(name("dagger.Binds"))
                    .addAnnotation(name("dagger.multibindings.IntoMap"))
                    .addAnnotation(name(API + "lifecycle.DynamicViewModelMap"))
                    .addAnnotation(AnnotationSpec.builder(name("dagger.multibindings.ClassKey"))
                            .addMember("value", "$T.class", ClassName.get(model)).build())
                    .addParameter(ClassName.get(model), "model")
                    .returns(name("androidx.lifecycle.ViewModel")).build());
        }
        return module.build();
    }

    private TypeSpec.Builder graphType(ClassName type, String key, String annotation, String scope) {
        TypeSpec.Builder builder = TypeSpec.interfaceBuilder(type.simpleName()).addModifiers(PUBLIC)
                .addAnnotation(name(scope));
        if (!key.equals(FEATURE)) {
            builder.addAnnotation(AnnotationSpec.builder(name(annotation))
                    .addMember("modules", "$L", classArray(modules.get(key))).build());
        }
        entryPoints.get(key).forEach(builder::addSuperinterface);
        return builder;
    }

    private MethodSpec builderGetter(String method, ClassName child) {
        return MethodSpec.methodBuilder(method).addModifiers(PUBLIC, ABSTRACT)
                .addAnnotation(Override.class).returns(child.nestedClass("Builder")).build();
    }

    private TypeSpec builder(ClassName type, ClassName contract, ClassName binding, String parameter) {
        ClassName builder = type.nestedClass("Builder");
        TypeSpec.Builder result = TypeSpec.interfaceBuilder("Builder").addModifiers(PUBLIC, STATIC)
                .addSuperinterface(contract.nestedClass("Builder"))
                .addAnnotation(name("dagger.Subcomponent.Builder"))
                .addMethod(MethodSpec.methodBuilder("build").addModifiers(PUBLIC, ABSTRACT)
                        .addAnnotation(Override.class).returns(type).build());
        if (binding != null) {
            result.addMethod(MethodSpec.methodBuilder(parameter).addModifiers(PUBLIC, ABSTRACT)
                    .addAnnotation(Override.class).addAnnotation(name("dagger.BindsInstance"))
                    .addParameter(binding, parameter).returns(builder).build());
        }
        return result.build();
    }

    private TypeSpec rootFactory(ClassName feature, List<TypeMirror> dependencies) {
        MethodSpec.Builder create = MethodSpec.methodBuilder("create")
                .addModifiers(PUBLIC, ABSTRACT).returns(feature);
        create.addParameter(ParameterSpec.builder(name("android.app.Application"), "application")
                .addAnnotation(name("dagger.BindsInstance")).build());
        create.addParameter(ParameterSpec.builder(name("android.content.Context"), "context")
                .addAnnotation(name("dagger.BindsInstance"))
                .addAnnotation(name("dagger.hilt.android.qualifiers.ApplicationContext")).build());
        for (int i = 0; i < dependencies.size(); i++) {
            create.addParameter(TypeName.get(dependencies.get(i)), "dependency" + i);
        }
        return TypeSpec.interfaceBuilder("Factory").addModifiers(PUBLIC, STATIC)
                .addAnnotation(name("dagger.Component.Factory")).addMethod(create.build()).build();
    }

    private void generateWrapper(List<TypeMirror> dependencies, List<TypeElement> viewModels) throws IOException {
        ClassName manager = name(API + "managers.DynamicFeatureComponentManager");
        ClassName dagger = ClassName.get(graph.packageName(), "Dagger" + graph.simpleName() + "_FeatureC");
        List<CodeBlock> arguments = new ArrayList<>();
        arguments.add(CodeBlock.of("application"));
        arguments.add(CodeBlock.of("application"));
        for (TypeMirror dependency : dependencies) {
            arguments.add(CodeBlock.of("$T.fromApplication(application, $T.class)",
                    name("dagger.hilt.android.EntryPointAccessors"), TypeName.get(dependency)));
        }
        TypeSpec wrapperType = TypeSpec.classBuilder(wrapper).addModifiers(PUBLIC, FINAL)
                .addOriginatingElement(root)
                .addMethod(MethodSpec.constructorBuilder().addModifiers(PRIVATE).build())
                .addField(FieldSpec.builder(manager, "MANAGER", PRIVATE, STATIC, FINAL)
                        .initializer("new $T()", manager).build())
                .addMethod(MethodSpec.methodBuilder("getGeneratedComponent").addModifiers(PUBLIC, STATIC)
                        .addParameter(name("android.content.Context"), "context")
                        .returns(component("DynamicFeatureComponent"))
                        .addStatement("return MANAGER.get(context, application -> $T.factory().create($L))",
                                dagger, CodeBlock.join(arguments, ", ")).build())
                .addMethod(MethodSpec.methodBuilder("viewModelFactory").addModifiers(PUBLIC, STATIC)
                        .addParameter(component("DynamicViewModelComponent").nestedClass("Builder"), "builder")
                        .addParameter(name("androidx.lifecycle.ViewModelProvider.Factory"), "delegate")
                        .returns(name("androidx.lifecycle.ViewModelProvider.Factory"))
                        .addStatement("return new $T(new Class<?>[] $L, builder, delegate)",
                                name(API + "lifecycle.DynamicViewModelFactory"),
                                classArray(viewModels.stream().map(ClassName::get).map(it -> (TypeName) it).toList()))
                        .build())
                .build();
        write(wrapper.packageName(), wrapperType);
    }

    private CodeBlock classArray(List<TypeName> types) {
        return CodeBlock.of("{$L}", CodeBlock.join(
                types.stream().map(type -> CodeBlock.of("$T.class", type)).toList(), ", "));
    }

    private void write(String packageName, TypeSpec type) throws IOException {
        JavaFile.builder(packageName, type).skipJavaLangImports(true).build().writeTo(env.getFiler());
    }
}
