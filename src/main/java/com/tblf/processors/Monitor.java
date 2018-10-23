package com.tblf.processors;

import com.tblf.linker.Calls;

import java.util.HashSet;
import java.util.Set;

public class Monitor {

    public static Set<String> called;

    static {
        called = new HashSet<>();
    }

    public Monitor(String methodQN) {
        if (!called.contains(methodQN)) {
            Calls.getTracer().write(methodQN.concat("\n"));
            called.add(methodQN);
        }
    }

    public void report(String methodQN) {
        if (!called.contains(";".concat(methodQN))) {
            Calls.getTracer().write(";".concat(methodQN).concat("\n"));
            called.add(";".concat(methodQN));
        }

    }
}

