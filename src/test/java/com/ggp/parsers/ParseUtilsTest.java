package com.ggp.parsers;

import org.junit.jupiter.api.Test;



import static org.junit.jupiter.api.Assertions.*;

class ParseUtilsTest {

    @Test
    void testParseConfigKey() throws Exception {
        ConfigKey key = ParseUtils.parseConfigExpression("Test{1,\"a\\t \\\" \\\\\", a = true, b=null, c=A{1}, d=[1,2,3,4]}").getConfigKey();
        assertEquals("Test", key.getName());
        assertEquals(1, key.getPositionalParams().get(0).getInt());
        assertEquals("a\t \" \\", key.getPositionalParams().get(1).getString());
        assertEquals(true, key.getKvParams().get("a").getBool());
        assertEquals(null, key.getKvParams().get("b").getConfigKey());
        assertEquals("A", key.getKvParams().get("c").getConfigKey().getName());
        assertEquals(1, key.getKvParams().get("c").getConfigKey().getPositionalParams().get(0).getInt());
        assertArrayEquals(new int[] {1,2,3,4}, key.getKvParams().get("d").getPrimitiveArray(int[].class));
    }
}