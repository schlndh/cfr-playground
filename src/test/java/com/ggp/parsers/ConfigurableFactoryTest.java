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

    private ConfigurableFactory createFactory() throws NoSuchMethodException {
        ConfigurableFactory factory = new ConfigurableFactory();
        factory.register(IntA.class, "A1", ConfigurableFactory.createPositionalParameterList(A1.class.getConstructor(int.class, int.class, IntB.class)));
        {
            HashMap<String, Parameter> kvParams = new HashMap<>();
            kvParams.put("b", new Parameter(IntB.class, null, false));
            factory.register(IntA.class, "A1",
                    new ParameterList(
                            Arrays.asList(
                                    new Parameter(int.class, 0, true),
                                    new Parameter(int.class, 9, false)
                            ),
                            kvParams,
                            (pos, kv) -> new A1((int) pos.get(0), (int) pos.get(1), (IntB) kv.get("b"))
                    )
            );
        }

        factory.register(IntA.class, "A2", ConfigurableFactory.createPositionalParameterList(A2.class.getConstructor(int.class, int.class, IntB.class)));
        {
            HashMap<String, Parameter> params = new HashMap<>();
            params.put("x", new Parameter(int.class, 1, false));
            params.put("y", new Parameter(int.class, 2, false));
            factory.register(IntB.class, "B1", new ParameterList(null, params,
                    (pos, kv) -> new B1((int) kv.get("x"), (int) kv.get("y"))));
        }
        return factory;
    }

    @Test
    void testCreate_correct() throws Exception {
        ConfigurableFactory factory = createFactory();

        assertEquals(new A1(5, 7, null), factory.create(IntA.class, ParseUtils.parseConfigKey("A1{5, 7, null}")));
        assertEquals(new A1(5, 9, null), factory.create(IntA.class, ParseUtils.parseConfigKey("A1{5}")));
        assertEquals(new A2(5, 7, null), factory.create(IntA.class, ParseUtils.parseConfigKey("A2{5, 7, null}")));
        assertEquals(new A1(5, 7, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigKey("A1{5, 7, B1{x=1,y=2}}")));
        assertEquals(new A1(5, 7, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigKey("A1{5, 7, B1}")));
        assertEquals(new A1(5, 7, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigKey("A1{5, 7, b=B1{x=1,y=2}}")));
        assertEquals(new A1(5, 9, new B1(1, 2)), factory.create(IntA.class, ParseUtils.parseConfigKey("A1{5, b=B1{x=1,y=2}}")));
    }
}