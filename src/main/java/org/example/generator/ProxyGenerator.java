package org.example.generator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProxyGenerator implements Generator {
    private final Generator valueGenerator;

    public ProxyGenerator(Generator valueGenerator) {
        this.valueGenerator = Objects.requireNonNull(valueGenerator, "Value generator must not be null");
    }

    @Override
    public Object generate(Type type) {
        var resolved = resolve(type, Map.of());
        var clazz = resolved.clazz();
        validate(clazz);

        return Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class<?>[] { clazz },
            (p, m, args) -> {
                if (m.getDeclaringClass() == Object.class) {
                    return switch (m.getName()) {
                        case "toString" -> clazz.getSimpleName() + "$proxy";
                        case "hashCode" -> System.identityHashCode(p);
                        case "equals" -> p == args[0];
                        default -> throw new UnsupportedOperationException(m.getName());
                    };
                }
                if (m.isDefault()) return InvocationHandler.invokeDefault(p, m, args);
                if (m.getReturnType() == void.class) return null;

                var returnType = m.getGenericReturnType();
                var bindings = new HashMap<TypeVariable<?>, ResolvedType>();
                if (!(returnType instanceof Class<?>)) collectBindings(resolved, bindings);
                var resultType = resolve(returnType, bindings);
                return valueGenerator.generate(resultType.bindings().isEmpty() ? resultType.clazz() : resultType);
            }
        );
    }

    private void collectBindings(ResolvedType type, Map<TypeVariable<?>, ResolvedType> bindings) {
        bindings.putAll(type.bindings());
        for (var parent : type.clazz().getGenericInterfaces()) {
            collectBindings(resolve(parent, type.bindings()), bindings);
        }
    }

    private void validate(Class<?> target) {
        if (target == null) throw new IllegalArgumentException();
        if (!target.isInterface()) throw new IllegalArgumentException();
    }
}
