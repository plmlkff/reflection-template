package org.example.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public abstract class TypeRef<T> {
    public Type getType() {
        if (getClass().getGenericSuperclass() instanceof ParameterizedType parameterized) {
            return parameterized.getActualTypeArguments()[0];
        }
        throw new IllegalArgumentException("TypeRef must specify a type argument");
    }
}
