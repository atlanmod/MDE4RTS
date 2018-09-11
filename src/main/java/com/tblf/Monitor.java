package com.tblf;

import com.tblf.linker.Calls;

import java.util.logging.Logger;

public class Monitor {
    private static final Logger LOGGER = Logger.getLogger("Monitor");

    public Monitor(String methodQN) {
        Calls.getTracer().write(methodQN.concat("\n"));
    }

    public void report(String methodQN) {
        Calls.getTracer().write(";".concat(methodQN).concat("\n"));
    }

}

