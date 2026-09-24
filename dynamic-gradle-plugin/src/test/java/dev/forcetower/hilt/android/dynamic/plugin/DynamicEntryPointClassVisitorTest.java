package dev.forcetower.hilt.android.dynamic.plugin;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class DynamicEntryPointClassVisitorTest {
    @Test(expected = IllegalStateException.class)
    public void rejectsKotlinDefaultArgumentSuperConstructors() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "test/Screen", null, "test/Base", null);
        MethodVisitor init = input.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitInsn(Opcodes.ICONST_0);
        init.visitInsn(Opcodes.ACONST_NULL);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "test/Base", "<init>",
                "(ILkotlin/jvm/internal/DefaultConstructorMarker;)V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(3, 1);
        init.visitEnd();
        input.visitEnd();
        new ClassReader(input.toByteArray()).accept(
                new DynamicEntryPointClassVisitor(new ClassWriter(0)), ClassReader.EXPAND_FRAMES);
    }

    @Test
    public void rewritesSuperWithoutRewritingObjectsConstructedAsArguments() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "test/Screen", null, "test/Base", null);
        MethodVisitor init = input.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitTypeInsn(Opcodes.NEW, "test/Base");
        init.visitInsn(Opcodes.DUP);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "test/Base", "<init>", "()V", false);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "test/Base", "<init>", "(Ltest/Base;)V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(3, 1);
        init.visitEnd();
        MethodVisitor create = input.visitMethod(Opcodes.ACC_PUBLIC, "onCreate", "()V", null, null);
        create.visitCode();
        create.visitVarInsn(Opcodes.ALOAD, 0);
        create.visitMethodInsn(Opcodes.INVOKESPECIAL, "test/Base", "onCreate", "()V", false);
        create.visitInsn(Opcodes.RETURN);
        create.visitMaxs(1, 1);
        create.visitEnd();
        input.visitEnd();

        List<String> calls = new ArrayList<>();
        List<String> parents = new ArrayList<>();
        new ClassReader(input.toByteArray()).accept(new DynamicEntryPointClassVisitor(
                new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(int version, int access, String name, String signature,
                            String superName, String[] interfaces) {
                        parents.add(superName);
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor,
                            String signature, String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitMethodInsn(int opcode, String owner, String method,
                                    String descriptor, boolean isInterface) {
                                calls.add(owner + "." + method);
                            }
                        };
                    }
                }), ClassReader.EXPAND_FRAMES);
        assertEquals(List.of("test/DynamicHilt_Screen"), parents);
        assertEquals(List.of("test/Base.<init>", "test/DynamicHilt_Screen.<init>",
                "test/DynamicHilt_Screen.onCreate"), calls);
    }
}
