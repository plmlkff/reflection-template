package org.example.generator;

import org.example.generator.provider.InterfaceImplementationProvider;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class AbstractGenerator implements Generator {
    private final Generator primitiveGenerator;
    private final InterfaceImplementationProvider interfaceImplementationProvider;

    public AbstractGenerator(Generator primitiveGenerator, InterfaceImplementationProvider interfaceImplementationProvider) {
        this.primitiveGenerator = primitiveGenerator;
        this.interfaceImplementationProvider = interfaceImplementationProvider;
    }

    @Override
    public Object generate(Class<?> clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        validateClass(clazz);

        if (clazz.isPrimitive()) return primitiveGenerator.generate(clazz);
        if (clazz.isInterface()) clazz = interfaceImplementationProvider.provide(clazz);

        var constructor = getConstructor(clazz);

        var args = prepareArgs(constructor);

        return constructor.newInstance(args);
    }

    private void validateClass(Class<?> target) {
        if (target == null) throw new IllegalArgumentException();
    }

    private Constructor<?> getConstructor(Class<?> clazz) {
        var constructors = Arrays.stream(clazz.getConstructors())
            .filter(c -> Arrays.stream(c.getParameterTypes()).noneMatch(t -> t == clazz))
            .toArray(Constructor[]::new);

        if (constructors.length == 0) throw new IllegalStateException();

        return constructors[ThreadLocalRandom.current().nextInt(constructors.length)];
    }
    private Object[] prepareArgs(Constructor<?> constructor) throws OperationNotSupportedException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var argTypes = constructor.getParameterTypes();
        var args = new Object[argTypes.length];

        for (int i = 0; i < argTypes.length; i++) {
            var type = argTypes[i];
            args[i] = generate(type);
        }

        return args;
    }
}
