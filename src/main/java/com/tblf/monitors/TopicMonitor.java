package com.tblf.monitors;

import com.tblf.linker.Calls;
import net.bytebuddy.asm.Advice;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;

public class TopicMonitor {

    public static String runningTest;
    public static Collection<String> methodsCalled;

    static {
        runningTest = "";
        methodsCalled = new HashSet<>();
    }

    @Advice.OnMethodEnter
    static void enter(@Advice.Origin Method method) throws IOException {
        Calls.getTracer().write(MonitorUtils.getMethodQualifiedName(method), runningTest); //May need opt
    }

    //called at the end of the method
    @Advice.OnMethodExit
    static void exit(@Advice.Origin Method method) throws IOException {

    }

}
