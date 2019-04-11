package com.tblf.monitors;

import net.bytebuddy.asm.Advice;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;

public class TopicTestMonitor {

    @Advice.OnMethodEnter
    static void enter(@Advice.Origin Method method) throws IOException {
        TopicMonitor.methodsCalled = new HashSet<>();
        TopicMonitor.runningTest = MonitorUtils.getMethodQualifiedName(method);
    }

    //called at the end of the method
    @Advice.OnMethodExit
    static void exit(@Advice.Origin Method method) throws IOException {
        TopicMonitor.runningTest = null;
    }
}
