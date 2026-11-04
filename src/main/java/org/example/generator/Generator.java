package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;

public interface Generator {
    Object generate(Class<?> clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException;
}
