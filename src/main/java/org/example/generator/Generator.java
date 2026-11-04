package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

@FunctionalInterface
public interface Generator {
    Object generate(Type type) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException;

    default ResolvedType resolve(Type type, Map<TypeVariable<?>, ResolvedType> bindings) {
        return switch (type) {
            case null -> throw new IllegalArgumentException("Type must not be null");
            case ResolvedType resolved -> resolved;
            case Class<?> clazz -> new ResolvedType(clazz, Map.of());
            case TypeVariable<?> variable -> {
                var resolved = bindings.get(variable);
                if (resolved == null) throw new IllegalArgumentException("Unresolved type variable: " + variable);
                yield resolved;
            }
            case ParameterizedType parameterized -> {
                var clazz = (Class<?>) parameterized.getRawType();
                if (parameterized.getOwnerType() instanceof ParameterizedType && !Modifier.isStatic(clazz.getModifiers())) {
                    throw new UnsupportedOperationException("Parameterized owner is unsupported: " + type.getTypeName());
                }
                var variables = clazz.getTypeParameters();
                var arguments = parameterized.getActualTypeArguments();
                var resolvedBindings = new HashMap<TypeVariable<?>, ResolvedType>();
                for (int i = 0; i < variables.length; i++) {
                    resolvedBindings.put(variables[i], resolve(arguments[i], bindings));
                }
                yield new ResolvedType(clazz, resolvedBindings);
            }
            default -> throw new UnsupportedOperationException("Unsupported type: " + type.getTypeName());
        };
    }

    record ResolvedType(Class<?> clazz, Map<TypeVariable<?>, ResolvedType> bindings) implements Type {
        public ResolvedType {
            bindings = Map.copyOf(bindings);
        }
    }
}
