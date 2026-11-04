package org.example;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.example.classes.Triangle;
import org.example.generator.TypeRef;
import org.example.generator.util.GeneratorUtils;

import javax.naming.OperationNotSupportedException;
import java.io.ObjectInputFilter;
import java.lang.reflect.InvocationTargetException;
import java.util.Random;
import java.util.function.Supplier;

public class Main {
    public static void main(String[] args) throws OperationNotSupportedException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var generator = GeneratorUtils.buildAbstractGenerator();

        System.out.println(generator.generate(BinaryTreeNode.class));
        System.out.println(generator.generate(Cart.class));
        System.out.println(generator.generate(Example.class));
        System.out.println(generator.generate(Product.class));
        System.out.println(generator.generate(Rectangle.class));
        System.out.println(generator.generate(Shape.class));
        System.out.println(generator.generate(Triangle.class));
        System.out.println(generator.generate(ObjectInputFilter.Status.class));

        System.out.println(generator.generate(new TypeRef<String>(){}.getType()));
        System.out.println(generator.generate(Random.class));
        System.out.println(((Supplier<String>)generator.generate(new TypeRef<Supplier<String>>(){}.getType())).get());
        System.out.println(((Shape)generator.generate(Shape.class)).getPerimeter());
    }
}
