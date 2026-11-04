package org.example.generator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class ProxyGenerator implements Generator {
    @Override
    public Object generate(Class<?> clazz) {
        validate(clazz);

        return Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] { clazz },
            (p, m, args) -> {
                if (m.isDefault()) return InvocationHandler.invokeDefault(p, m, args);
                return switch (m.getName()) {
                    case "toString" -> clazz.getSimpleName() + "$proxy";
                    case "hashCode" -> System.identityHashCode(p);
                    case "equals" -> p == args[0];
                    default -> generate(m.getReturnType());
                };
            }
        );
    }

    private void validate(Class<?> target) {
        if (target == null) throw new IllegalArgumentException();
        if (!target.isInterface()) throw new IllegalArgumentException();
    }
}
