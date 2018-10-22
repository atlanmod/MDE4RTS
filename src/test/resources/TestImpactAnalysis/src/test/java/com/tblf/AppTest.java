package com.tblf;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    @Test
    public void testDirectCall( ) {
        new App().method();
    }

    @Test
    public void testParentCall( ) {
        new com.tblf.ParentClass().method();
    }

    @Test
    public void testSuperCall() {
        com.tblf.ParentClass parentClass = new App();
        parentClass.method();
    }

    @Test
    public void testSuperMethodCalledFromChildClass() {
        new App().otherMethod();
    }

    @Test
    public void testMultipleCall() {
        new App().method();
        new App().method();
    }
}
