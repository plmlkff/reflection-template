package org.example.generator;

import org.example.classes.Triangle;

import java.lang.reflect.Type;
import java.util.concurrent.ThreadLocalRandom;

public class TriangleGenerator implements Generator {
    @Override
    public Triangle generate(Type type) {
        var random = ThreadLocalRandom.current();
        var sideA = random.nextDouble(1, 100);
        var sideB = random.nextDouble(1, 100);
        var sideC = random.nextDouble(Math.abs(sideA - sideB) + 0.1, sideA + sideB - 0.1);
        return new Triangle(sideA, sideB, sideC);
    }
}
