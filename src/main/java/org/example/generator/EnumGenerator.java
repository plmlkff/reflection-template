package org.example.generator;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class EnumGenerator implements Generator {
    @Override
    public Object generate(Type type) {
        var clazz = resolve(type, Map.of()).clazz();
        if (!clazz.isEnum()) throw new IllegalArgumentException("Expected an enum type: " + clazz.getTypeName());

        var constants = clazz.getEnumConstants();
        if (constants.length == 0) throw new IllegalArgumentException("Enum has no constants: " + clazz.getTypeName());
        return constants[ThreadLocalRandom.current().nextInt(constants.length)];
    }
}
