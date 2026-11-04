package org.example.generator.provider;

import java.util.Map;

public class InterfaceImplementationProvider {
    private final Map<Class<?>, Class<?>> interface2Impl;

    public InterfaceImplementationProvider(Map<Class<?>, Class<?>> interface2Impl) {
        this.interface2Impl = interface2Impl;
    }

    public Class<?> provide(Class<?> interfaceClass) {
        validate(interfaceClass);

        var implClass = interface2Impl.get(interfaceClass);

        if (implClass == null) throw new UnsupportedOperationException();

        return implClass;
    }

    private static void validate(Class<?> interfaceClass) {
        if (interfaceClass == null) throw new IllegalArgumentException();
        if (!interfaceClass.isInterface()) throw new IllegalArgumentException();
    }
}
