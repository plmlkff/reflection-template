package org.example.generator;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AbstractGenerator implements Generator {
    private final Generator primitiveGenerator;
    private final Generator proxyGenerator;
    private final Generator arrayGenerator;
    private final GenericGenerator genericGenerator;
    private final Map<Class<?>, Generator> type2specificGenerator;

    public AbstractGenerator(Generator primitiveGenerator, Map<Set<Class<?>>, Generator> types2specificGenerator,
                             Function<Generator, Map<Class<?>, Generator>> genericSpecificFactory) {
        this.primitiveGenerator = primitiveGenerator;
        this.proxyGenerator = new ProxyGenerator(this);
        this.arrayGenerator = new ArrayGenerator(this);
        this.genericGenerator = new GenericGenerator(this, genericSpecificFactory.apply(this));
        this.type2specificGenerator = types2specificGenerator.entrySet().stream()
            .flatMap(e -> e.getKey().stream().map(t -> Map.entry(t, e.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public Object generate(Type type) throws InvocationTargetException, InstantiationException, IllegalAccessException, OperationNotSupportedException {
        var resolved = resolve(type, Map.of());
        var clazz = resolved.clazz();

        var specificGenerator = type2specificGenerator.get(clazz);
        if (specificGenerator != null) {
            return specificGenerator.generate(resolved.bindings().isEmpty() ? clazz : resolved);
        }

        if (clazz.isPrimitive()) return primitiveGenerator.generate(clazz);
        if (clazz.isArray()) return arrayGenerator.generate(resolved);
        if (clazz.isInterface() && !genericGenerator.hasSpecificGenerator(clazz)) {
            return proxyGenerator.generate(resolved);
        }

        return genericGenerator.generate(resolved);
    }
}
