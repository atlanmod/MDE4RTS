package com.tblf;

import java.lang.instrument.Instrumentation;

/**
 * Hello world!
 *
 */
public class Agent
{
    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("Premain called! ");
    }
}
