package org.example;

import org.example.classes.BinaryTreeNode;
import org.example.generator.util.GeneratorUtils;

import javax.naming.OperationNotSupportedException;
import java.lang.reflect.InvocationTargetException;

public class Main {
    public static void main(String[] args) throws OperationNotSupportedException, InvocationTargetException, InstantiationException, IllegalAccessException {
        var generator = GeneratorUtils.buildAbstractGenerator();

        System.out.println(generator.generate(BinaryTreeNode.class));
    }
}
