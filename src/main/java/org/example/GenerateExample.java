package org.example;


import org.example.classes.Example;
import org.example.generator.SimpleGenerator;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new SimpleGenerator();
        try {
            Object generated = gen.generate(Example.class);
            System.out.println(generated);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}