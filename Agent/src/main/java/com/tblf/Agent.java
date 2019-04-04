package com.tblf;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

/**
 * Hello world!
 *
 */
public class Agent
{
    public static void premain(String args, Instrumentation instrumentation) {
        new AgentBuilder.Default()
                .with(new AgentBuilder.InitializationStrategy.SelfInjection.Eager())
                .type(ElementMatchers.nameStartsWith("com.tblf"))
                .transform((builder, typeDescription, classLoader, javaModule)
                        -> builder.method(ElementMatchers.any()).intercept(Advice.to(Interceptor.class)))
                .installOn(instrumentation);
    }

    static class Interceptor {
        @Advice.OnMethodEnter
        static void enter(@Advice.Origin Method method) {
            System.out.println("Im in "+method.toGenericString());
        }

        //called at the end of the method
        @Advice.OnMethodExit
        static void exit(@Advice.Origin Method method) {
            System.out.println("Im out "+method.toGenericString());
        }
    }
}
