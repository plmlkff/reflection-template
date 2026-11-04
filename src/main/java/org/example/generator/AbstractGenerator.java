package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AbstractGenerator implements Generator {
    private final Generator primitiveGenerator;
    private final Generator proxyGenerator;
    private final Generator arrayGenerator;
    private final GenericGenerator genericGenerator;
    private final Map<Class<?>, Generator> type2specificGenerator;

    public AbstractGenerator(Generator primitiveGenerator, Map<Set<Class<?>>, Generator> types2specificGenerator,
                             Function<Generator, Map<Class<?>, Generator>> genericSpecificFactory) {
        this.primitiveGenerator = primitiveGenerator;
        this.proxyGenerator = new ProxyGenerator(this);
        this.arrayGenerator = new ArrayGenerator(this);
        this.genericGenerator = new GenericGenerator(this, genericSpecificFactory.apply(this));
        this.type2specificGenerator = types2specificGenerator.entrySet().stream()
            .flatMap(e -> e.getKey().stream().map(t -> Map.entry(t, e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Object generate(Type type) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        return generate(type, new HashSet<>());
    }

    private Object generate(Type type, Set<Class<?>> annotationPath) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        var resolved = resolve(type, Map.of());
        var clazz = resolved.clazz();

        var annotation = clazz.getDeclaredAnnotation(Generatable.class);
        if (annotation != null && (annotation.target() != void.class || annotation.generator() != Generator.class)) {
            return generateAnnotated(resolved, annotation, annotationPath);
        }

        var specificGenerator = type2specificGenerator.get(clazz);
        if (specificGenerator != null) {
            return specificGenerator.generate(resolved.bindings().isEmpty() ? clazz : resolved);
        }

        if (clazz.isPrimitive()) return primitiveGenerator.generate(clazz);
        if (clazz.isArray()) return arrayGenerator.generate(resolved);
        if (clazz.isInterface() && !genericGenerator.hasSpecificGenerator(clazz)) {
            return proxyGenerator.generate(resolved);
        }

        return genericGenerator.generate(resolved);
    }

    private Object generateAnnotated(ResolvedType requested, Generatable annotation, Set<Class<?>> annotationPath) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        var clazz = requested.clazz();
        if (!annotationPath.add(clazz)) {
            throw new IllegalArgumentException("Cyclic @Generatable configuration: " + clazz.getTypeName());
        }

        try {
            var effective = getResolvedType(requested, annotation, clazz);

            Object value;
            if (annotation.generator() != Generator.class) {
                var generatorClass = annotation.generator();
                var instance = generate(generatorClass, annotationPath);
                if (!generatorClass.isInstance(instance)) {
                    throw new IllegalStateException("Could not create @Generatable generator: " + generatorClass.getTypeName());
                }
                value = ((Generator) instance).generate(effective.bindings().isEmpty() ? effective.clazz() : effective);
            } else {
                value = generate(effective, annotationPath);
            }

            if (!clazz.isInstance(value) || !effective.clazz().isInstance(value)) {
                throw new IllegalStateException("@Generatable must produce " + effective.clazz().getTypeName() + ", got " + (value == null ? "null" : value.getClass().getTypeName()));
            }
            return value;
        } finally {
            annotationPath.remove(clazz);
        }
    }

    private static ResolvedType getResolvedType(ResolvedType requested, Generatable annotation, Class<?> clazz) {
        var effective = requested;
        if (annotation.target() == void.class) return effective;

        var target = annotation.target();
        if (!clazz.isAssignableFrom(target)) {
            throw new IllegalArgumentException("@Generatable target " + target.getTypeName() + " is not assignable to " + clazz.getTypeName());
        }
        if (target.getTypeParameters().length != 0 || !requested.bindings().isEmpty()) {
            throw new UnsupportedOperationException("Generic @Generatable redirection is unsupported: " + clazz.getTypeName());
        }
        effective = new ResolvedType(target, Map.of());

        return effective;
    }
}
