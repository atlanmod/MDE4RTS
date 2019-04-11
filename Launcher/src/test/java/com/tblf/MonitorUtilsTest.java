package com.tblf;

import com.tblf.monitors.MonitorUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class MonitorUtilsTest {

    @Test
    public void getMethodQualifiedNameCheck() {
        List<Method> methodList = Arrays.asList(this.getClass().getMethods());
        Method method = methodList.stream().filter(m -> m.getName().equals("getMethodQualifiedNameCheck")).findFirst().get();

        Assert.assertEquals("com.tblf.MonitorUtilsTest$getMethodQualifiedNameCheck", MonitorUtils.getMethodQualifiedName(method));

    }
}
