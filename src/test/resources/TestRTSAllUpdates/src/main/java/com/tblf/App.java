package com.tblf;

/**
 * Hello world!
 *
 */
public class App 
{
    public void foo()
    {
        System.out.println( "Hello Warld!" );
    }

    public void bar() {
        foo();
    }

    public void methodThatWillBeOverriden() {
        System.out.println("My child will override this. How rude! ");
    }

    public void methodThatIsOverriden() {
        System.out.println("My child is overriding this. It thinks it is better than me !");
    }

    public static class AppSubClass {
        public void subMethod() {
            System.out.println("This method has been updated !");
        }
    }
}
