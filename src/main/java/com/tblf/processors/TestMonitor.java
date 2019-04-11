package com.tblf.processors;

import com.tblf.linker.Calls;

import java.util.HashSet;

public class TestMonitor {
    public TestMonitor(String methodQN) {
        Monitor.called = new HashSet<>();
        Calls.getTracer().write(":".concat(methodQN).concat("\n"));
    }

    public TestMonitor() {

    }

    public void report(String methodQN) {
        Calls.getTracer().write(";".concat(methodQN).concat("\n"));
    }
}

