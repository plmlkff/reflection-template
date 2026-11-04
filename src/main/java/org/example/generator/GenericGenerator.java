package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GenericGenerator implements Generator {
    private static final double NULL_RATIO = 0.5d;

    private final Generator valueGenerator;
    private final Map<Class<?>, Generator> type2specificGenerator;

    public GenericGenerator(Generator valueGenerator, Map<Class<?>, Generator> type2specificGenerator) {
        this.valueGenerator = valueGenerator;
        this.type2specificGenerator = Map.copyOf(type2specificGenerator);
    }

    public boolean hasSpecificGenerator(Class<?> clazz) {
        return type2specificGenerator.containsKey(clazz);
    }

    @Override
    public Object generate(Type type) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        var resolved = resolve(type, Map.of());
        var clazz = resolved.clazz();

        var specificGenerator = type2specificGenerator.get(clazz);
        if (specificGenerator != null) return specificGenerator.generate(resolved);

        var constructor = getConstructor(clazz);
        var parameters = constructor.getGenericParameterTypes();
        var arguments = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            var parameter = resolve(parameters[i], resolved.bindings());
            if (!parameter.clazz().isPrimitive() && ThreadLocalRandom.current().nextDouble() < NULL_RATIO) {
                arguments[i] = null;
            } else {
                arguments[i] = valueGenerator.generate(parameter);
            }
        }
        return constructor.newInstance(arguments);
    }

    private Constructor<?> getConstructor(Class<?> clazz) {
        var constructors = clazz.getConstructors();
        if (constructors.length == 0) throw new IllegalArgumentException("No public constructors: " + clazz.getTypeName());
        return constructors[ThreadLocalRandom.current().nextInt(constructors.length)];
    }
}
