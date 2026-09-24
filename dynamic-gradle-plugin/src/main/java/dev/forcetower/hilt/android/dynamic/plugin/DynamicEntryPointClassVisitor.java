package dev.forcetower.hilt.android.dynamic.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;

final class DynamicEntryPointClassVisitor extends ClassVisitor {
    private String className;
    private String oldParent;
    private String newParent;

    DynamicEntryPointClassVisitor(ClassVisitor next) {
        super(Opcodes.ASM9, next);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        className = name;
        oldParent = superName;
        int separator = name.lastIndexOf('/') + 1;
        newParent = name.substring(0, separator) + "DynamicHilt_"
                + name.substring(separator).replace('$', '_');
        super.visit(version, access, name,
                signature == null ? null : signature.replace(oldParent, newParent),
                newParent, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
            String signature, String[] exceptions) {
        MethodVisitor next = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (oldParent.equals(newParent)) {
            return next;
        }
        return new AnalyzerAdapter(Opcodes.ASM9, className, access, name, descriptor, next) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String method,
                    String desc, boolean isInterface) {
                boolean rewrite = opcode == Opcodes.INVOKESPECIAL && owner.equals(oldParent);
                if (rewrite && method.equals("<init>")) {
                    int arguments = 0;
                    for (Type argument : Type.getArgumentTypes(desc)) {
                        arguments += argument.getSize();
                    }
                    int receiver = stack == null ? -1 : stack.size() - arguments - 1;
                    rewrite = receiver >= 0 && stack.get(receiver) == Opcodes.UNINITIALIZED_THIS;
                    if (rewrite && desc.contains("Lkotlin/jvm/internal/DefaultConstructorMarker;")) {
                        throw new IllegalStateException(
                                "Dynamic Hilt cannot rewrite a Kotlin default-argument super constructor in "
                                        + className + ". Use an explicit base class and extend the generated class.");
                    }
                }
                super.visitMethodInsn(opcode, rewrite ? newParent : owner, method, desc, isInterface);
            }
        };
    }
}
