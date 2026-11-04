package org.example.generator.util;

import org.example.generator.AbstractGenerator;
import org.example.generator.Generator;
import org.example.generator.PrimitiveGenerator;
import org.example.generator.provider.InterfaceImplementationProvider;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GeneratorUtils {
    public static Generator buildAbstractGenerator() {
        return new AbstractGenerator(
            buildPrimitiveGenerator(),
            buildInterfaceImplProvider()
        );
    }

    public static Generator buildPrimitiveGenerator() {
        return new PrimitiveGenerator(
            Map.of(
                short.class, () -> (short)ThreadLocalRandom.current().nextInt(Short.MIN_VALUE, Short.MAX_VALUE + 1),
                int.class, () -> ThreadLocalRandom.current().nextInt(),
                long.class, () -> ThreadLocalRandom.current().nextLong(),
                double.class, () -> ThreadLocalRandom.current().nextDouble(),
                float.class, () -> ThreadLocalRandom.current().nextFloat(),
                boolean.class, () -> ThreadLocalRandom.current().nextBoolean(),
                byte.class, () -> (byte)ThreadLocalRandom.current().nextInt(Byte.MIN_VALUE, Byte.MAX_VALUE + 1),
                char.class, () -> (char)ThreadLocalRandom.current().nextInt(Character.MIN_VALUE, Character.MAX_VALUE + 1)
            )
        );
    }

    public static InterfaceImplementationProvider buildInterfaceImplProvider() {

    }
}
