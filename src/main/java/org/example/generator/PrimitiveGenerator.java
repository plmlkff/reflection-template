package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.util.Map;
import java.util.function.Supplier;

public class PrimitiveGenerator implements Generator {
    private final Map<Class<?>, Supplier<?>> valueSuppliersByType;

    public PrimitiveGenerator(Map<Class<?>, Supplier<?>> valueSuppliersByType) {
        this.valueSuppliersByType = valueSuppliersByType;
    }

    @Override
    public Object generate(Class<?> clazz) throws OperationNotSupportedException {
        validateClass(clazz);

        var valueGenerator = valueSuppliersByType.get(clazz);
        if (valueGenerator == null) throw new OperationNotSupportedException();

        return valueGenerator.get();
    }

    private static void validateClass(Class<?> clazz) {
        if (clazz == null) throw new IllegalArgumentException();
        if (!clazz.isPrimitive())  throw new IllegalArgumentException();
    }
}
