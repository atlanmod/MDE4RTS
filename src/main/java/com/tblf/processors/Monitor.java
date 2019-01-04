package com.tblf.processors;

import com.tblf.linker.Calls;

import java.util.HashSet;
import java.util.Set;

public class Monitor {

    static Set<String> called;

    static {
        called = new HashSet<>();
    }

    public Monitor(String methodQN) {
        if (!called.contains(methodQN)) {
            System.out.println("Called");
            Calls.getTracer().write(methodQN.concat("\n"));
            called.add(methodQN);
        }
    }

    public Monitor() {

    }

    public void report(String methodQN) {
        if (!called.contains(";".concat(methodQN))) {
            Calls.getTracer().write(";".concat(methodQN).concat("\n"));
            called.add(";".concat(methodQN));
        }

    }
}

