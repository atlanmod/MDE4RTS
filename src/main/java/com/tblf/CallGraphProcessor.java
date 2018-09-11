package com.tblf;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

public class CallGraphProcessor extends ClassVisitor {
    private String className;

    public CallGraphProcessor() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name.replace("/", ".");
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);

        return new AdviceAdapter(this.api, methodVisitor, access, name, desc) {
            @Override
            public void visitCode() {
                super.visitCode();
            }

            int value;
            boolean isTest = false;

            @Override
            public AnnotationVisitor visitAnnotation(String s, boolean b) {
                if (s.equals("Lorg/junit/Test;")) {
                    //This is a test method
                    isTest = true;
                }
                return super.visitAnnotation(s, b);
            }


            @Override
            protected void onMethodEnter() {
                super.onMethodEnter();
                if (isTest) {
                    mv.visitTypeInsn(Opcodes.NEW, "com/tblf/TestMonitor");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn(className + "$" + name);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/tblf/TestMonitor", "<init>", "(Ljava/lang/String;)V", false);

                    value = newLocal(Type.getType(TestMonitor.class));
                    mv.visitVarInsn(Opcodes.ASTORE, value);
                } else {
                    mv.visitTypeInsn(Opcodes.NEW, "com/tblf/Monitor");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn(className + "$" + name);
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/tblf/Monitor", "<init>", "(Ljava/lang/String;)V", false);

                    value = newLocal(Type.getType(Monitor.class));
                    mv.visitVarInsn(Opcodes.ASTORE, value);
                }
            }

            @Override
            protected void onMethodExit(int opcode) {
                if (isTest) {
                    mv.visitVarInsn(Opcodes.ALOAD, value);
                    mv.visitLdcInsn(className + "$" + name);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/tblf/TestMonitor", "report", "(Ljava/lang/String;)V", false);
                } else {
                    mv.visitVarInsn(Opcodes.ALOAD, value);
                    mv.visitLdcInsn(className + "$" + name);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/tblf/Monitor", "report", "(Ljava/lang/String;)V", false);
                }

                super.onMethodExit(opcode);
            }
        };
    }
}
