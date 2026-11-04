package org.example.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Random;

public class SimpleGenerator implements Generator {

    @Override
    public Object generate(Type type) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        var clazz = resolve(type, Map.of()).clazz();
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length == 0) throw new IllegalArgumentException("No constructors: " + clazz.getTypeName());

        int randomConstructorIndex = new Random().nextInt(constructors.length);
        Constructor<?> randomConstructor = constructors[randomConstructorIndex];
        return randomConstructor.newInstance(111);
    }


}
