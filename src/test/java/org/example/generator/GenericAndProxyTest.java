package org.example.generator;

import org.example.generator.util.GeneratorUtils;
import org.junit.jupiter.api.Test;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GenericAndProxyTest {
    private final Generator generator = GeneratorUtils.buildAbstractGenerator();

    public static class Leaf { public Leaf() {} }
    public static class Other { public Other() {} }
    public record Box<T>(T value) {}
    public record Nested<T>(Box<Box<T>> value) {}
    public record UsesBox(Box<Leaf> value) {}
    public record ArrayBox<T>(T[] values) {}
    public static class LocalVariable { public <U> LocalVariable(U value) {} }
    public static class PrivateConstructor { private PrivateConstructor() {} }
    public static class Broken {
        public Broken() { throw new IllegalStateException("Fixture constructor failed"); }
    }
    public enum Empty {}
    public interface Special<T> { T value(); }
    public interface Parent<T> { T value(); }
    public interface Child<U> extends Parent<List<U>> {}
    public interface FixedChild extends Parent<Leaf> {}
    public interface Service<T> {
        T value();
        Box<T> boxed();
        void touch();
        int count();
        <U> U unknown();
        default String label() { return "default"; }
    }

    @Test
    void resolvesNestedBindingsWithoutMixingTypeVariables() {
        var variable = Box.class.getTypeParameters()[0];
        var nested = generator.resolve(new TypeRef<Box<Box<Leaf>>>() {}.getType(), Map.of());
        assertSame(Box.class, nested.bindings().get(variable).clazz());
        assertSame(Leaf.class, nested.bindings().get(variable).bindings().get(variable).clazz());
        var owner = generator.resolve(new TypeRef<Nested<Other>>() {}.getType(), Map.of());
        var parameter = Nested.class.getConstructors()[0].getGenericParameterTypes()[0];
        var resolved = generator.resolve(parameter, owner.bindings());
        assertSame(Other.class, resolved.bindings().get(variable).bindings().get(variable).clazz());
        assertSame(Leaf.class, nested.bindings().get(variable).bindings().get(variable).clazz());
        assertSame(resolved, generator.resolve(resolved, Map.of()));
    }

    @Test
    void genericAndOrdinaryConstructorsKeepParameterizedArguments() throws Exception {
        boolean nestedObserved = false, ordinaryObserved = false;
        for (int i = 0; i < 100; i++) {
            var nested = (Box<?>) generator.generate(new TypeRef<Box<Box<Leaf>>>() {}.getType());
            nestedObserved |= checkBox(nested.value(), Leaf.class);
            var ordinary = (UsesBox) generator.generate(UsesBox.class);
            ordinaryObserved |= checkBox(ordinary.value(), Leaf.class);
        }
        assertTrue(nestedObserved && ordinaryObserved);
    }

    @Test
    void nestedCollectionsUseDeclaredElementKeyAndValueTypes() throws Exception {
        var map = (Map<?, ?>) generator.generate(new TypeRef<Map<Leaf, List<Other>>>() {}.getType());
        assertFalse(map.isEmpty());
        for (var entry : map.entrySet()) {
            assertInstanceOf(Leaf.class, entry.getKey());
            var values = assertInstanceOf(List.class, entry.getValue());
            assertFalse(values.isEmpty());
            for (Object value : values) assertInstanceOf(Other.class, value);
        }
    }

    @Test
    void registeredGenericHandlersWorkForNewInterfacesAndConcreteTypes() throws Exception {
        var leaf = new Leaf();
        Special<Leaf> expectedInterface = () -> leaf;
        var expectedBox = new Box<>(List.of(leaf));
        Generator unused = type -> { throw new AssertionError("Unexpected primitive generation"); };
        Generator custom = new AbstractGenerator(unused, Map.of(), values -> Map.of(
                Special.class, type -> {
                    var resolved = values.resolve(type, Map.of());
                    assertSame(Leaf.class, resolved.bindings().get(Special.class.getTypeParameters()[0]).clazz());
                    return expectedInterface;
                },
                Box.class, type -> {
                    var resolved = values.resolve(type, Map.of());
                    var argument = resolved.bindings().get(Box.class.getTypeParameters()[0]);
                    assertSame(List.class, argument.clazz());
                    assertSame(Leaf.class, argument.bindings().get(List.class.getTypeParameters()[0]).clazz());
                    return expectedBox;
                }));
        assertSame(expectedInterface, custom.generate(new TypeRef<Special<Leaf>>() {}.getType()));
        assertSame(expectedBox, custom.generate(new TypeRef<Box<List<Leaf>>>() {}.getType()));
    }

    @Test
    void proxiesSupportGenericReturnsDefaultObjectAndVoidMethods() throws Exception {
        var service = (Service<?>) generator.generate(new TypeRef<Service<Leaf>>() {}.getType());
        assertInstanceOf(Leaf.class, service.value());
        boolean observed = false;
        for (int i = 0; i < 50; i++) observed |= checkBox(service.boxed(), Leaf.class);
        assertTrue(observed);
        service.touch();
        assertDoesNotThrow(service::count);
        assertEquals("default", service.label());
        assertEquals(service, service);
        assertNotEquals(service, new Object());
        assertEquals(System.identityHashCode(service), service.hashCode());
        assertNotNull(service.toString());
        assertMeaningful(assertThrows(IllegalArgumentException.class, service::unknown));
    }

    @Test
    void proxiesResolveGenericParentInterfaces() throws Exception {
        var child = (Child<?>) generator.generate(new TypeRef<Child<Leaf>>() {}.getType());
        var values = assertInstanceOf(List.class, child.value());
        assertFalse(values.isEmpty());
        for (Object value : values) assertInstanceOf(Leaf.class, value);
        var fixed = (FixedChild) generator.generate(FixedChild.class);
        assertInstanceOf(Leaf.class, fixed.value());
    }

    @Test
    void unresolvedAndUnsupportedTypesFailWithMeaningfulMessages() {
        assertMeaningful(assertThrows(IllegalArgumentException.class, () -> generator.generate(null)));
        assertMeaningful(assertThrows(IllegalArgumentException.class, () -> generator.generate(Box.class)));
        assertMeaningful(assertThrows(IllegalArgumentException.class, () -> generator.generate(LocalVariable.class)));
        assertMeaningful(assertThrows(IllegalArgumentException.class, () -> generator.generate(PrivateConstructor.class)));
        assertMeaningful(assertThrows(IllegalArgumentException.class, () -> generator.generate(Empty.class)));
        assertMeaningful(assertThrows(UnsupportedOperationException.class,
                () -> generator.generate(new TypeRef<Box<? extends Leaf>>() {}.getType())));
        assertMeaningful(assertThrows(UnsupportedOperationException.class,
                () -> generator.generate(new TypeRef<ArrayBox<Leaf>>() {}.getType())));
    }

    @Test
    void specializedGeneratorsRejectWrongCategories() {
        assertMeaningful(assertThrows(IllegalArgumentException.class,
                () -> GeneratorUtils.buildPrimitiveGenerator().generate(String.class)));
        assertMeaningful(assertThrows(OperationNotSupportedException.class,
                () -> new PrimitiveGenerator(Map.of()).generate(int.class)));
        assertMeaningful(assertThrows(IllegalArgumentException.class,
                () -> new ArrayGenerator(generator).generate(String.class)));
        assertMeaningful(assertThrows(IllegalArgumentException.class,
                () -> new ProxyGenerator(generator).generate(String.class)));
        assertMeaningful(assertThrows(IllegalArgumentException.class,
                () -> new EnumGenerator().generate(String.class)));
    }

    @Test
    void constructorFailureKeepsItsOriginalCause() {
        var error = assertThrows(InvocationTargetException.class, () -> generator.generate(Broken.class));
        var cause = assertInstanceOf(IllegalStateException.class, error.getCause());
        assertEquals("Fixture constructor failed", cause.getMessage());
    }

    private static boolean checkBox(Object value, Class<?> expected) {
        if (value == null) return false;
        var box = assertInstanceOf(Box.class, value);
        if (box.value() == null) return false;
        assertSame(expected, box.value().getClass());
        return true;
    }

    private static void assertMeaningful(Exception error) {
        assertNotNull(error.getMessage());
        assertFalse(error.getMessage().isBlank());
    }
}

