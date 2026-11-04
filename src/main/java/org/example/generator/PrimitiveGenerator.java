package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.function.Supplier;

public class PrimitiveGenerator implements Generator {
    private final Map<Class<?>, Supplier<?>> valueSuppliersByType;

    public PrimitiveGenerator(Map<Class<?>, Supplier<?>> valueSuppliersByType) {
        this.valueSuppliersByType = valueSuppliersByType;
    }

    @Override
    public Object generate(Type type) throws OperationNotSupportedException {
        var clazz = resolve(type, Map.of()).clazz();
        validateClass(clazz);

        var valueGenerator = valueSuppliersByType.get(clazz);
        if (valueGenerator == null) throw new OperationNotSupportedException("No generator for primitive type: " + clazz.getTypeName());

        return valueGenerator.get();
    }

    private static void validateClass(Class<?> clazz) {
        if (clazz == null) throw new IllegalArgumentException("Type must not be null");
        if (!clazz.isPrimitive()) throw new IllegalArgumentException("Expected a primitive type: " + clazz.getTypeName());
    }
}
