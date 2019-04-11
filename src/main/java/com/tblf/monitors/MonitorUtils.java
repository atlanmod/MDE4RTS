package com.tblf.monitors;

import java.lang.reflect.Method;

public class MonitorUtils {
    public static String getMethodQualifiedName(Method method) {
        return method.getDeclaringClass().getName().concat("$").concat(method.getName());
    }
}
