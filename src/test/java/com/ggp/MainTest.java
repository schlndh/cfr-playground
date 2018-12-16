package com.ggp;

import com.ggp.parsers.ConfigurableFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void testRegisterClassesToFactory() throws Exception {
        ConfigurableFactory factory = new ConfigurableFactory();
        Main.registerClassesToFactory(factory);
    }
}