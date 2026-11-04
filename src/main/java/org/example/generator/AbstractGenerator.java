package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AbstractGenerator implements Generator {
    private static final double NULL_RATIO = 0.4d;

    private final Generator primitiveGenerator;
    private final Generator proxyGenerator;
    private final Generator arrayGenerator;
    private final Map<Class<?>, Generator> type2specificGenerator;

    public AbstractGenerator(Generator primitiveGenerator, Generator proxyGenerator, Map<Set<Class<?>>, Generator> types2specificGenerator) {
        this.primitiveGenerator = primitiveGenerator;
        this.proxyGenerator = proxyGenerator;
        this.arrayGenerator = new ArrayGenerator(this);
        this.type2specificGenerator = types2specificGenerator.entrySet().stream()
            .flatMap(e -> e.getKey().stream().map(t -> Map.entry(t, e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Object generate(Class<?> clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        validateClass(clazz);

        var specificGenerator = type2specificGenerator.get(clazz);
        if (specificGenerator != null) return specificGenerator.generate(clazz);

        if (clazz.isPrimitive()) return primitiveGenerator.generate(clazz);
        if (clazz.isArray()) return arrayGenerator.generate(clazz);
        if (clazz.isInterface()) return proxyGenerator.generate(clazz);

        var constructor = getConstructor(clazz);

        var args = prepareArgs(constructor);

        return constructor.newInstance(args);
    }

    private void validateClass(Class<?> target) {
        if (target == null) throw new IllegalArgumentException();
    }

    private Constructor<?> getConstructor(Class<?> clazz) {
        var constructors = clazz.getConstructors();

        if (constructors.length == 0) throw new IllegalArgumentException();

        return constructors[ThreadLocalRandom.current().nextInt(constructors.length)];
    }
    private Object[] prepareArgs(Constructor<?> constructor) throws OperationNotSupportedException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var argTypes = constructor.getParameterTypes();
        var args = new Object[argTypes.length];

        for (int i = 0; i < argTypes.length; i++) {
            var type = argTypes[i];
            if (!type.isPrimitive() && ThreadLocalRandom.current().nextDouble() < NULL_RATIO) args[i] = null;
            else args[i] = generate(type);
        }

        return args;
    }
}
