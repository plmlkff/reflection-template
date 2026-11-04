package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ArrayGenerator implements Generator {
    private static final int MAX_ARRAY_LENGTH = 10;

    private final Generator valueGenerator;

    public ArrayGenerator(Generator valueGenerator) {
        this.valueGenerator = valueGenerator;
    }

    @Override
    public Object generate(Type type) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        var clazz = resolve(type, Map.of()).clazz();
        validate(clazz);

        var length = ThreadLocalRandom.current().nextInt(MAX_ARRAY_LENGTH);
        var compType = clazz.getComponentType();
        var array = Array.newInstance(compType, length);

        for (int i = 0; i < length; i++) {
            Array.set(array, i, valueGenerator.generate(compType));
        }

        return array;
    }

    private void validate(Class<?> target) {
        if (target == null) throw new IllegalArgumentException();
        if (!target.isArray()) throw new IllegalArgumentException();
    }
}
