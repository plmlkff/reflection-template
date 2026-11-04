package org.example.generator;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.generator.util.GeneratorUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    private final Generator generator = GeneratorUtils.buildAbstractGenerator();

    public enum Color { RED, GREEN, BLUE }

    public record Measurement(int count, double total, boolean enabled) {}

    static Stream<Arguments> primitiveTypes() {
        return Stream.of(
                Arguments.of(boolean.class, Boolean.class),
                Arguments.of(byte.class, Byte.class),
                Arguments.of(short.class, Short.class),
                Arguments.of(int.class, Integer.class),
                Arguments.of(long.class, Long.class),
                Arguments.of(float.class, Float.class),
                Arguments.of(double.class, Double.class),
                Arguments.of(char.class, Character.class)
        );
    }

    static Stream<Class<?>> arrayTypes() {
        return Stream.of(int[].class, String[].class, Color[].class, Rectangle[].class, Integer[][].class);
    }

    @ParameterizedTest
    @MethodSource("primitiveTypes")
    void generatesPrimitiveValuesAndTheirWrappers(Class<?> primitive, Class<?> wrapper) throws Exception {
        for (int i = 0; i < 20; i++) {
            assertInstanceOf(wrapper, generator.generate(primitive));
            assertInstanceOf(wrapper, generator.generate(wrapper));
        }
    }

    @Test
    void generatesNonemptyStrings() throws Exception {
        String value = assertInstanceOf(String.class, generator.generate(String.class));

        assertFalse(value.isEmpty());
    }

    @Test
    void choosesOneOfTheEnumConstants() throws Exception {
        Color value = assertInstanceOf(Color.class, generator.generate(Color.class));

        assertTrue(Arrays.asList(Color.values()).contains(value));
    }

    @ParameterizedTest
    @MethodSource("arrayTypes")
    void generatesArraysWithTheRequestedComponentType(Class<?> arrayType) throws Exception {
        Object array = generator.generate(arrayType);

        assertSame(arrayType, array.getClass());
        Class<?> componentType = arrayType.getComponentType();
        for (int i = 0; i < Array.getLength(array); i++) {
            Object value = Array.get(array, i);
            if (componentType == int.class) {
                assertInstanceOf(Integer.class, value);
            } else {
                assertInstanceOf(componentType, value);
            }
        }
    }

    @Test
    void generatesNonemptyRawCollections() throws Exception {
        List<?> list = assertInstanceOf(List.class, generator.generate(List.class));
        Map<?, ?> map = assertInstanceOf(Map.class, generator.generate(Map.class));

        assertFalse(list.isEmpty());
        assertFalse(map.isEmpty());
        list.forEach(value -> assertInstanceOf(Integer.class, value));
        map.forEach((key, value) -> {
            assertInstanceOf(Integer.class, key);
            assertInstanceOf(Integer.class, value);
        });
    }

    @Test
    void fillsAParameterizedListWithProducts() throws Exception {
        List<?> items = assertInstanceOf(List.class,
                generator.generate(new TypeRef<List<Product>>() {}.getType()));

        assertFalse(items.isEmpty());
        items.forEach(item -> assertInstanceOf(Product.class, item));
    }

    @Test
    void createsNewClassesUsingTheirConstructorParameters() throws Exception {
        Measurement measurement = assertInstanceOf(Measurement.class, generator.generate(Measurement.class));

        assertTrue(Double.isFinite(measurement.total()));
    }

    @Test
    void createsTheClassesFromTheTemplate() throws Exception {
        Rectangle rectangle = assertInstanceOf(Rectangle.class, generator.generate(Rectangle.class));
        Product product = assertInstanceOf(Product.class, generator.generate(Product.class));

        assertInstanceOf(Example.class, generator.generate(Example.class));
        assertTrue(Double.isFinite(rectangle.getArea()));
        assertTrue(rectangle.getArea() >= 0);
        assertTrue(Double.isFinite(rectangle.getPerimeter()));
        assertTrue(Double.isFinite(product.getPrice()));
        if (product.getName() != null) assertFalse(product.getName().isEmpty());
    }

    @Test
    void cartItemsAreProductsWhenTheConstructorArgumentIsNotNull() throws Exception {
        boolean generatedItems = false;

        for (int i = 0; i < 40; i++) {
            Cart cart = assertInstanceOf(Cart.class, generator.generate(Cart.class));
            List<?> items = cart.getItems();
            if (items == null) continue;

            generatedItems = true;
            assertFalse(items.isEmpty());
            items.forEach(item -> assertInstanceOf(Product.class, item));
        }

        assertTrue(generatedItems, "Expected at least one cart with generated items");
    }

    @Test
    void recursiveConstructorParametersAreDelegatedToTheValueGenerator() throws Exception {
        var leaf = new BinaryTreeNode(1, null, null);
        Generator values = type -> {
            var requested = generator.resolve(type, Map.of());
            return requested.clazz() == BinaryTreeNode.class ? leaf : generator.generate(type);
        };

        var tree = (BinaryTreeNode) new GenericGenerator(values, Map.of()).generate(BinaryTreeNode.class);

        assertNotSame(leaf, tree);
        if (tree.getData() != null) assertInstanceOf(Integer.class, tree.getData());
        if (tree.getLeft() != null) assertSame(leaf, tree.getLeft());
        if (tree.getRight() != null) assertSame(leaf, tree.getRight());
    }

    @Test
    void simpleGeneratorCreatesTheIntroductoryExample() throws Exception {
        assertInstanceOf(Example.class, new SimpleGenerator().generate(Example.class));
    }
}
