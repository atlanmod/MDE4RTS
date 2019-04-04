package com.tblf.processors;

import java.util.HashSet;

public class TopicTestMonitor {

    public TopicTestMonitor(String methodQN) {
        TopicMonitor.methodsCalled = new HashSet<>();
        TopicMonitor.runningTest = methodQN;
    }

    public void report(String methodQN) {

    }

}
