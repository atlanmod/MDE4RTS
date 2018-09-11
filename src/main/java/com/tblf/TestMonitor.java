package com.tblf;

import com.tblf.linker.Calls;

import java.util.logging.Logger;

public class TestMonitor {
    private static final Logger LOGGER = Logger.getLogger("Monitor");

    public TestMonitor(String methodQN) {
        Calls.getTracer().write(":".concat(methodQN));
    }

    public void report(String methodQN) {
        Calls.getTracer().write(";".concat(methodQN).concat("\n"));
    }

}

