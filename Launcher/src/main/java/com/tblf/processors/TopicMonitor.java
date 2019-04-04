package com.tblf.processors;

import com.tblf.linker.Calls;

import java.util.Collection;
import java.util.HashSet;

public class TopicMonitor {

    static String runningTest;
    static Collection<String> methodsCalled;

    static {
        runningTest = "";
        methodsCalled = new HashSet<>();
    }

    public TopicMonitor(String methodQN) {
        if (!methodsCalled.contains(methodQN)) {
                Calls.getTracer().write(methodQN, runningTest);
        }
    }

    public void report(String methodQN) {

    }

}
