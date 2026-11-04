package org.example.generator.util;

import org.example.generator.AbstractGenerator;
import org.example.generator.Generator;
import org.example.generator.PrimitiveGenerator;
import org.example.generator.ProxyGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class GeneratorUtils {
    private static final int MIN_STRING_LENGTH = 1;
    private static final int MAX_STRING_LENGTH = 128;
    private static final int MIN_COLLECTION_SIZE = 1;
    private static final int MAX_COLLECTION_SIZE = 128;

    public static Generator buildAbstractGenerator() {
        return new AbstractGenerator(
            buildPrimitiveGenerator(),
            buildProxyGenerator(),
            buildSpecificGenerators()
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

    public static Generator buildProxyGenerator() {
        return new ProxyGenerator();
    }

    public static Map<Set<Class<?>>, Generator> buildSpecificGenerators() {
        var primitiveGenerator = buildPrimitiveGenerator();
        Map<Class<?>, Class<?>> wrapper2primitive = Map.of(
            Boolean.class, boolean.class,
            Byte.class, byte.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class,
            Character.class, char.class
        );

        return Map.of(
            wrapper2primitive.keySet(), clazz -> primitiveGenerator.generate(wrapper2primitive.get(clazz)),
            Set.of(String.class), new RandomStringGenerator(),
            Set.of(List.class), new RandomListGenerator(),
            Set.of(Map.class), new RandomMapGenerator()
        );
    }

    private static class RandomStringGenerator implements Generator {
        public String generate(Class<?> clazz) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            var length = rnd.nextInt(MIN_STRING_LENGTH, MAX_STRING_LENGTH);
            var sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append((char)rnd.nextInt(Character.MIN_VALUE, Character.MAX_VALUE));
            }
            return sb.toString();
        }
    }

    private static class RandomListGenerator implements Generator {
        public List<Integer> generate(Class<?> clazz) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            var size = rnd.nextInt(MIN_COLLECTION_SIZE, MAX_COLLECTION_SIZE);
            var list = new ArrayList<Integer>(size);
            for (int i = 0; i < size; i++) {
                list.add(rnd.nextInt());
            }
            return list;
        }
    }

    private static class RandomMapGenerator implements Generator {
        public Map<Integer, Integer> generate(Class<?> clazz) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            var size = rnd.nextInt(MIN_COLLECTION_SIZE, MAX_COLLECTION_SIZE);
            var list = new HashMap<Integer, Integer>();
            for (int i = 0; i < size; i++) {
                list.put(rnd.nextInt(), rnd.nextInt());
            }
            return list;
        }
    }
}
