package com.tblf;

import com.tblf.linker.Calls;

import java.util.HashSet;
import java.util.logging.Logger;

public class TestMonitor {
    public TestMonitor(String methodQN) {
        Monitor.called = new HashSet<>();
        Calls.getTracer().write(":".concat(methodQN).concat("\n"));
    }

    public void report(String methodQN) {
        Calls.getTracer().write(";".concat(methodQN).concat("\n"));
    }
}

