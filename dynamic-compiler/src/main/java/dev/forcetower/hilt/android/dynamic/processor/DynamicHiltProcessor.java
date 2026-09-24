package dev.forcetower.hilt.android.dynamic.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public final class DynamicHiltProcessor extends AbstractProcessor {
    static final String API = "dev.forcetower.hilt.android.dynamic.";
    static final String ROOT = API + "DeclareHiltDynamicFeature";
    static final String ENTRY = API + "DynamicAndroidEntryPoint";
    static final String INSTALL = "dagger.hilt.InstallIn";
    static final String FEATURE = API + "components.DynamicFeatureComponent";
    static final String RETAINED = "dagger.hilt.android.components.ActivityRetainedComponent";
    static final String ACTIVITY = "dagger.hilt.android.components.ActivityComponent";
    static final String FRAGMENT = "dagger.hilt.android.components.FragmentComponent";
    static final String VIEWMODEL = "dagger.hilt.android.components.ViewModelComponent";
    static final String HILT_VIEWMODEL = "dagger.hilt.android.lifecycle.HiltViewModel";
    static final String SINGLETON = "dagger.hilt.components.SingletonComponent";

    private final Map<String, TypeElement> roots = new LinkedHashMap<>();
    private final Map<String, TypeElement> entries = new LinkedHashMap<>();
    private final Map<String, TypeElement> installed = new LinkedHashMap<>();
    private final Map<String, TypeElement> viewModels = new LinkedHashMap<>();
    private final Set<String> generatedBases = new java.util.HashSet<>();
    private boolean generatedGraph;
    private boolean failed;
    private int round;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ROOT, ENTRY, INSTALL, HILT_VIEWMODEL);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of("dynamic.hilt.enableTransform");
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
        if (environment.processingOver()) {
            List<TypeElement> declarations = new ArrayList<>(entries.values());
            declarations.addAll(viewModels.values());
            declarations.addAll(installed.values());
            if (!failed && !declarations.isEmpty() && roots.isEmpty()) {
                error(declarations.get(0),
                        "Declare exactly one @DeclareHiltDynamicFeature in this feature module.");
            }
            return false;
        }
        boolean changed = collect(environment, ROOT, roots);
        changed |= collect(environment, ENTRY, entries);
        changed |= collect(environment, INSTALL, installed);
        changed |= collect(environment, HILT_VIEWMODEL, viewModels);
        if (roots.size() > 1) {
            error(roots.values().iterator().next(),
                    "Only one @DeclareHiltDynamicFeature is allowed per compilation.");
        }
        if (failed || roots.isEmpty()) {
            return false;
        }
        if (generatedGraph && changed) {
            error(roots.values().iterator().next(),
                    "Dynamic Hilt declarations were generated after the feature graph. "
                            + "Declare entry points and installed modules in source.");
            return false;
        }
        TypeElement root = roots.values().iterator().next();
        try {
            FeatureGenerator generator = new FeatureGenerator(this, processingEnv, root);
            for (TypeElement entry : entries.values()) {
                if (generatedBases.add(entry.getQualifiedName().toString())) {
                    generator.generateBase(entry);
                }
            }
            if (!failed && !generatedGraph) {
                if (changed) {
                    // Advance a round so modules produced by other processors can be collected.
                    JavaFile.builder(packageName(root), TypeSpec.classBuilder(
                                    flattened(root) + "_DynamicHiltRound" + round++)
                            .addOriginatingElement(root).build()).build().writeTo(processingEnv.getFiler());
                } else {
                    generator.generateGraph(new ArrayList<>(entries.values()),
                            new ArrayList<>(installed.values()), new ArrayList<>(viewModels.values()));
                    generatedGraph = true;
                }
            }
        } catch (IOException exception) {
            error(root, "Cannot write Dynamic Hilt sources: " + exception.getMessage());
        }
        return false;
    }

    private boolean collect(RoundEnvironment environment, String annotation, Map<String, TypeElement> into) {
        TypeElement type = processingEnv.getElementUtils().getTypeElement(annotation);
        boolean changed = false;
        if (type != null) {
            for (Element element : environment.getElementsAnnotatedWith(type)) {
                if (element instanceof TypeElement) {
                    TypeElement entry = (TypeElement) element;
                    changed |= into.putIfAbsent(entry.getQualifiedName().toString(), entry) == null;
                }
            }
        }
        return changed;
    }

    AnnotationMirror annotation(Element element, String name) {
        return element.getAnnotationMirrors().stream()
                .filter(it -> it.getAnnotationType().toString().equals(name)).findFirst().orElse(null);
    }

    AnnotationValue value(AnnotationMirror annotation, String name) {
        return processingEnv.getElementUtils().getElementValuesWithDefaults(annotation)
                .entrySet().stream().filter(it -> it.getKey().getSimpleName().contentEquals(name))
                .findFirst().orElseThrow().getValue();
    }

    List<TypeMirror> types(AnnotationMirror annotation, String name) {
        List<TypeMirror> result = new ArrayList<>();
        for (Object item : (List<?>) value(annotation, name).getValue()) {
            result.add((TypeMirror) ((AnnotationValue) item).getValue());
        }
        return result;
    }

    boolean publiclyAccessible(TypeElement type) {
        for (Element enclosing = type; enclosing instanceof TypeElement;
                enclosing = enclosing.getEnclosingElement()) {
            if (!enclosing.getModifiers().contains(Modifier.PUBLIC)) {
                return false;
            }
        }
        return true;
    }

    boolean validEntry(TypeElement entry) {
        if (entry.getKind() != ElementKind.CLASS || !entry.getTypeParameters().isEmpty()
                || !publiclyAccessible(entry)
                || (entry.getEnclosingElement() instanceof TypeElement
                && !entry.getModifiers().contains(Modifier.STATIC))) {
            error(entry, "@DynamicAndroidEntryPoint requires a public, non-generic class "
                    + "(nested classes must be static).");
            return false;
        }
        if (annotation(entry, "dagger.hilt.android.AndroidEntryPoint") != null) {
            error(entry, "Use @DynamicAndroidEntryPoint alone in a dynamic feature.");
            return false;
        }
        return true;
    }

    String packageName(TypeElement element) {
        return processingEnv.getElementUtils().getPackageOf(element).getQualifiedName().toString();
    }

    String flattened(TypeElement element) {
        return String.join("_", ClassName.get(element).simpleNames());
    }

    void error(Element element, String message) {
        failed = true;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    boolean failed() {
        return failed;
    }
}
