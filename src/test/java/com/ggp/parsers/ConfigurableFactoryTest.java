package com.ggp.parsers;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurableFactoryTest {

    private interface IntA {}
    private interface IntB {}
    private static class A1 implements IntA {
        private int x, y;
        private IntB b;

        public A1(int x, int y, IntB b) {
            this.x = x;
            this.y = y;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            A1 a1 = (A1) o;
            return x == a1.x &&
                    y == a1.y &&
                    Objects.equals(b, a1.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, b);
        }
    }

    private static class A2 extends A1 {
        public A2(int x, int y, IntB b) {
            super(x, y, b);
        }
    }

    private static class B1 implements IntB {
        private int x, y;

        public B1(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            B1 b1 = (B1) o;
            return x == b1.x &&
                    y == b1.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    private static class B2 implements IntB {
        private int[] x;

        public B2(int[] x) {
            this.x = x;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            B2 b2 = (B2) o;
            return Arrays.equals(x, b2.x);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(x);
        }
    }

    private ConfigurableFactory createFactory() throws NoSuchMethodException {
        ConfigurableFactory factory = new ConfigurableFactory();
        factory.register(IntA.class, "A1", ConfigurableFactory.createPositionalFactory(A1.class.getConstructor(int.class, int.class, IntB.class)));
        {
            HashMap<String, Parameter> kvParams = new HashMap<>();
            kvParams.put("b", new Parameter(IntB.class, null, false));
            factory.register(IntA.class, "A1",
                    new FactoryDescription(
                            Arrays.asList(
                                    new Parameter(int.class, 0, true),
                                    new Parameter(int.class, 9, false)
                            ),
                            kvParams,
                            (pos, kv) -> new A1((int) pos.get(0), (int) pos.get(1), (IntB) kv.get("b"))
                    )
            );
        }

        factory.register(IntA.class, "A2", ConfigurableFactory.createPositionalFactory(A2.class.getConstructor(int.class, int.class, IntB.class)));
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("x", new Parameter(int.class, 1, false));
            params.put("y", new Parameter(int.class, 2, false));
            factory.register(IntB.class, "B1", new FactoryDescription(null, params,
                    (pos, kv) -> new B1((int) kv.get("x"), (int) kv.get("y"))));
        }
        factory.register(IntB.class, "B2", ConfigurableFactory.createPositionalFactory(B2.class.getConstructor(int[].class)));
        return factory;
    }

    @Test
    void testCreate_correct() throws Exception {
        ConfigurableFactory factory = createFactory();
        assertEquals(new A1(5, 7, null), factory.create(IntA.class, ParseUtils.parseConfigExpression("A1{5, 7, null}")));
        assertEquals(new A1(5, 9, null), factory.create(IntA.class, ParseUtils.parseConfigExpression("A1{5}")));
        assertEquals(new A2(5, 7, null), factory.create(IntA.class, ParseUtils.parseConfigExpression("A2{5, 7, null}")));
        assertEquals(new A1(5, 7, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigExpression("A1{5, 7, B1{x=1,y=2}}")));
        assertEquals(new A1(5, 7, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigExpression("A1{5, 7, B1}")));
        assertEquals(new A1(5, 7, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigExpression("A1{5, 7, b=B1{x=1,y=2}}")));
        assertEquals(new A1(5, 9, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigExpression("A1{5, b=B1{x=1,y=2}}")));
        assertEquals(new A1(5, 7, new B2(new int[]{1,2,3})), factory.create(IntA.class, ParseUtils.parseConfigExpression("A1{5, 7, B2{[1,2,3]}}")));
    }

    @Test
    void testCreate_topLevelExpressions() throws Exception {
        ConfigurableFactory factory = createFactory();
        assertTrue(Arrays.deepEquals(new int[][]{{1,2,3},{4,5}},
                factory.create(int[][].class, ParseUtils.parseConfigExpression("[[1,2,3],[4,5]]"))));
        assertArrayEquals(new IntA[] {new A1(5, 7, null), new A2(1, 2, null)},
                factory.create(IntA[].class, ParseUtils.parseConfigExpression("[A1{5,7,null},A2{1,2,null}]")));
        assertEquals(100, (int) factory.create(int.class, ParseUtils.parseConfigExpression("100")));
        assertEquals(100L, (long) factory.create(long.class, ParseUtils.parseConfigExpression("100")));
        assertEquals("abc", factory.create(String.class, ParseUtils.parseConfigExpression("\"abc\"")));
        assertEquals(true, factory.create(Boolean.class, ParseUtils.parseConfigExpression("true")));
        assertEquals(null, factory.create(A1.class, ParseUtils.parseConfigExpression("null")));
        // null should work even for unregistered classes
        assertEquals(null, factory.create((new Object() {private int x = 5;}).getClass(), ParseUtils.parseConfigExpression("null")));

        assertEquals(Double.NEGATIVE_INFINITY, (double) factory.create(double.class, ParseUtils.parseConfigExpression("-Infinity")));
        assertEquals(Double.POSITIVE_INFINITY, (double) factory.create(double.class, ParseUtils.parseConfigExpression("Infinity")));
    }
}