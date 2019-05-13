package com.tblf;

import com.tblf.monitors.TopicMonitor;
import com.tblf.monitors.TopicTestMonitor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;


public class Agent
{
    public static void premain(String args, Instrumentation instrumentation) throws IOException {
        File target = new File(args);

        new AgentBuilder.Default()
                .type((typeDescription, classLoader, javaModule, aClass, protectionDomain) -> {
                    try {
                        File file = new File(protectionDomain.getCodeSource().getLocation().toURI().getPath());
                        return (file.getAbsolutePath().contains(target.getAbsolutePath()));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .transform((builder, typeDescription, classLoader, javaModule) ->
                            builder.method(ElementMatchers.isAnnotatedWith(Test.class)).intercept(Advice.to(TopicTestMonitor.class)))
                .transform((builder, typeDescription, classLoader, javaModule) ->
                            builder.method(ElementMatchers.not(ElementMatchers.isAnnotatedWith(Test.class))).intercept(Advice.to(TopicMonitor.class)))
                .installOn(instrumentation);
    }
}
