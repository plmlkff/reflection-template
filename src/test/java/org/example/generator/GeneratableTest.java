package org.example.generator;

import org.example.classes.Shape;
import org.example.classes.Triangle;
import org.example.generator.util.GeneratorUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GeneratableTest {
    private final Generator generator = GeneratorUtils.buildAbstractGenerator();

    @Generatable(target = InterfaceValue.class)
    public interface InterfaceSource {}
    public static class InterfaceValue implements InterfaceSource { public InterfaceValue() {} }
    @Generatable(target = AbstractValue.class)
    public abstract static class AbstractSource { public AbstractSource() {} }
    public static class AbstractValue extends AbstractSource { public AbstractValue() {} }
    @Generatable(target = ClassValue.class)
    public static class ClassSource { public ClassSource() {} }
    public static class ClassValue extends ClassSource { public ClassValue() {} }

    @Generatable(generator = SeededGenerator.class)
    public record SeededValue(int seed) {}
    public static class SeededGenerator implements Generator {
        private final int seed;
        public SeededGenerator(int seed) { this.seed = seed; }
        public Object generate(Type type) { return new SeededValue(seed); }
    }

    @Generatable(target = TargetValue.class, generator = TargetGenerator.class)
    public interface TargetSource {}
    public static class TargetValue implements TargetSource { public TargetValue() {} }
    public static class TargetGenerator implements Generator {
        public TargetGenerator() {}
        public Object generate(Type type) {
            assertSame(TargetValue.class, resolve(type, Map.of()).clazz());
            return new TargetValue();
        }
    }

    @Generatable(target = RequiredTarget.class, generator = WrongTargetGenerator.class)
    public interface WrongTargetSource {}
    public static class RequiredTarget implements WrongTargetSource { public RequiredTarget() {} }
    public static class OtherTarget implements WrongTargetSource { public OtherTarget() {} }
    public static class WrongTargetGenerator implements Generator {
        public WrongTargetGenerator() {}
        public Object generate(Type type) { return new OtherTarget(); }
    }

    @Generatable(target = String.class)
    public static class InvalidTarget { public InvalidTarget() {} }
    @Generatable(generator = BrokenGenerator.class)
    public static class WrongResult { public WrongResult() {} }
    @Generatable(generator = BrokenGenerator.class)
    public static class NullResult { public NullResult() {} }
    public static class BrokenGenerator implements Generator {
        public BrokenGenerator() {}
        public Object generate(Type type) {
            return resolve(type, Map.of()).clazz() == NullResult.class ? null : "wrong";
        }
    }

    @Generatable(target = Cycle.class)
    public static class Cycle { public Cycle() {} }
    @Generatable(generator = FactoryLoop.class)
    public static class FactoryLoop implements Generator {
        public FactoryLoop() {}
        public Object generate(Type type) { return this; }
    }

    @Generatable
    public static class DefaultValue { public DefaultValue() {} }
    @Generatable(generator = ParentGenerator.class)
    public static class AnnotatedParent { public AnnotatedParent() {} }
    public static class PlainChild extends AnnotatedParent { public PlainChild() {} }
    public static class ParentGenerator implements Generator {
        public ParentGenerator() {}
        public Object generate(Type type) { return new AnnotatedParent(); }
    }

    public static class Leaf { public Leaf() {} }
    @Generatable(generator = TypedGenerator.class)
    public interface Typed<T> { Class<?> argument(); }
    public record TypedValue(Class<?> argument) implements Typed<Leaf> {}
    public static class TypedGenerator implements Generator {
        public TypedGenerator() {}
        public Object generate(Type type) {
            var resolved = resolve(type, Map.of());
            return new TypedValue(resolved.bindings().get(Typed.class.getTypeParameters()[0]).clazz());
        }
    }

    @Generatable(target = GenericTarget.class)
    public interface GenericTargetSource {}
    public static class GenericTarget<T> implements GenericTargetSource { public GenericTarget() {} }
    @Generatable(target = FixedTarget.class)
    public interface GenericSource<T> {}
    public static class FixedTarget implements GenericSource<Leaf> { public FixedTarget() {} }

    @Test
    void targetCanSelectAnImplementationOrSubclass() throws Exception {
        assertInstanceOf(InterfaceValue.class, generator.generate(InterfaceSource.class));
        assertInstanceOf(AbstractValue.class, generator.generate(AbstractSource.class));
        assertInstanceOf(ClassValue.class, generator.generate(ClassSource.class));
    }

    @Test
    void customGeneratorIsBuiltByTheSameRouterAndOverridesRegistrations() throws Exception {
        Generator primitive = type -> {
            assertSame(int.class, type);
            return 37;
        };
        Generator unexpected = type -> { throw new AssertionError("Annotation must take priority"); };
        Generator custom = new AbstractGenerator(primitive,
                Map.of(Set.of(SeededValue.class), unexpected),
                values -> Map.of(SeededValue.class, unexpected));
        assertEquals(37, ((SeededValue) custom.generate(SeededValue.class)).seed());

        var supplied = new SeededGenerator(91);
        custom = new AbstractGenerator(primitive,
                Map.of(Set.of(SeededGenerator.class), type -> supplied), values -> Map.of());
        assertEquals(91, ((SeededValue) custom.generate(SeededValue.class)).seed());
    }

    @Test
    void customGeneratorReceivesEffectiveTargetAndGenericBindings() throws Exception {
        assertInstanceOf(TargetValue.class, generator.generate(TargetSource.class));
        var typed = (Typed<?>) generator.generate(new TypeRef<Typed<Leaf>>() {}.getType());
        assertSame(Leaf.class, typed.argument());
    }

    @Test
    void invalidTargetsAndCustomResultsHaveMeaningfulErrors() {
        var target = assertThrows(IllegalArgumentException.class, () -> generator.generate(InvalidTarget.class));
        assertTrue(target.getMessage().contains(InvalidTarget.class.getName()));
        assertTrue(target.getMessage().contains(String.class.getName()));
        assertMeaningful(assertThrows(IllegalStateException.class, () -> generator.generate(WrongResult.class)));
        assertMeaningful(assertThrows(IllegalStateException.class, () -> generator.generate(WrongTargetSource.class)));
        var missing = assertThrows(IllegalStateException.class, () -> generator.generate(NullResult.class));
        assertTrue(missing.getMessage().contains("null"));
    }

    @Test
    void emptyAnnotationIsANoopAndAnnotationsAreNotInherited() throws Exception {
        assertInstanceOf(DefaultValue.class, generator.generate(DefaultValue.class));
        assertSame(PlainChild.class, generator.generate(PlainChild.class).getClass());
    }

    @Test
    void annotationCyclesAreRejected() {
        for (Class<?> type : new Class<?>[] {Cycle.class, FactoryLoop.class}) {
            var error = assertThrows(IllegalArgumentException.class, () -> generator.generate(type));
            assertTrue(error.getMessage().contains("Cyclic"));
        }
    }

    @Test
    void unsupportedGenericRedirectionIsExplicit() {
        assertMeaningful(assertThrows(UnsupportedOperationException.class,
                () -> generator.generate(GenericTargetSource.class)));
        assertMeaningful(assertThrows(UnsupportedOperationException.class,
                () -> generator.generate(new TypeRef<GenericSource<Leaf>>() {}.getType())));
    }

    @Test
    void shapeAndTriangleRequestsProduceValidTriangles() throws Exception {
        var a = Triangle.class.getDeclaredField("sideA");
        var b = Triangle.class.getDeclaredField("sideB");
        var c = Triangle.class.getDeclaredField("sideC");
        a.setAccessible(true);
        b.setAccessible(true);
        c.setAccessible(true);
        for (int i = 0; i < 50; i++) {
            var triangle = assertInstanceOf(Triangle.class, generator.generate(i % 2 == 0 ? Shape.class : Triangle.class));
            double first = a.getDouble(triangle), second = b.getDouble(triangle), third = c.getDouble(triangle);
            assertTrue(first > 0 && second > 0 && third > 0);
            assertTrue(first + second > third && first + third > second && second + third > first);
            assertTrue(Double.isFinite(triangle.getArea()) && triangle.getArea() > 0);
        }
    }

    private static void assertMeaningful(Exception error) {
        assertNotNull(error.getMessage());
        assertFalse(error.getMessage().isBlank());
    }
}

