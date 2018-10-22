package com.tblf;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void testOne() {
        new App().foo();
    }

    @Test
    public void testTwo() {
        new App().bar();
    }

    @Test
    public void testThree() {
        Assert.assertEquals(50, 50);
    }

    @Test
    public void testFive() {
        new AppChild().methodThatWillBeOverriden();
    }

    @Test
    public void testSix() {
        new AppChild().methodThatIsOverriden();
    }

    @Test
    public void testSeven() {
        App.AppSubClass appSubClass = new App.AppSubClass();
        appSubClass.subMethod();
    }
}
