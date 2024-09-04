package com.zhongan.devpilot.util;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class PsiElementUtils {
    public static String getFullClassName(@NotNull PsiElement element) {
        if (element instanceof PsiMethod) {
            var psiClass = ((PsiMethod) element).getContainingClass();
            if (psiClass == null) {
                return null;
            }

            return psiClass.getQualifiedName();
        }

        return null;
    }

    public static String getRelatedClass(@NotNull PsiElement element) {
        Set<PsiClass> classSet = new HashSet<>();

        if (element instanceof PsiMethod) {
            classSet = getMethodRelatedClass(element);
        } else if (element instanceof PsiClass) {
            classSet = getClassRelatedClass(element);
        }

        var result = new StringBuilder();

        for (PsiClass psiClass : classSet) {
            if (ignoreClass(psiClass)) {
                continue;
            }
            result.append(psiClass.getText()).append("\n");
        }

        return result.toString();
    }

    private static Set<PsiClass> getClassRelatedClass(@NotNull PsiElement element) {
        Set<PsiClass> result = new HashSet<>();

        if (element instanceof PsiClass) {
            var psiClass = (PsiClass) element;
            var methods = psiClass.getMethods();
            var fields = psiClass.getFields();

            for (PsiMethod psiMethod : methods) {
                result.addAll(getMethodRelatedClass(psiMethod));
            }

            for (PsiField psiField : fields) {
                result.addAll(getFieldTypeClass(psiField));
            }
        }

        return result;
    }

    private static Set<PsiClass> getMethodRelatedClass(@NotNull PsiElement element) {
        var parameterClass = getMethodParameterTypeClass(element);
        var returnClass = getMethodReturnTypeClass(element);

        var result = new HashSet<>(parameterClass);
        result.addAll(returnClass);

        return result;
    }

    private static List<PsiClass> getMethodReturnTypeClass(@NotNull PsiElement element) {
        var result = new ArrayList<PsiClass>();

        if (element instanceof PsiMethod) {
            var returnType = ((PsiMethod) element).getReturnType();

            if (returnType instanceof PsiClassReferenceType) {
                var referenceType = (PsiClassReferenceType) returnType;
                var returnTypeClass = referenceType.resolve();
                result.addAll(getGenericType(referenceType));
                if (returnTypeClass != null) {
                    result.add(returnTypeClass);
                    return result;
                }
            }
        }

        return result;
    }

    private static List<PsiClass> getMethodParameterTypeClass(@NotNull PsiElement element) {
        var result = new ArrayList<PsiClass>();

        if (element instanceof PsiMethod) {
            var params = ((PsiMethod) element).getParameterList().getParameters();

            for (JvmParameter parameter : params) {
                if (parameter.getType() instanceof PsiClassReferenceType) {
                    var referenceType = (PsiClassReferenceType) parameter.getType();
                    var psiClass = referenceType.resolve();
                    if (psiClass != null) {
                        result.add(psiClass);
                    }
                    result.addAll(getGenericType(referenceType));
                }
            }
        }

        return result;
    }

    private static List<PsiClass> getFieldTypeClass(@NotNull PsiElement element) {
        var result = new ArrayList<PsiClass>();

        if (element instanceof PsiField) {
            var field = ((PsiField) element);

            if (field.getType() instanceof PsiClassReferenceType) {
                var referenceType = (PsiClassReferenceType) field.getType();
                var psiClass = referenceType.resolve();
                if (psiClass != null) {
                    result.add(psiClass);
                }
                result.addAll(getGenericType(referenceType));
            }
        }

        return result;
    }

    private static List<PsiClass> getGenericType(PsiClassReferenceType referenceType) {
        var result = new ArrayList<PsiClass>();

        var genericType = referenceType.resolveGenerics();
        var typeClass = genericType.getElement();

        if (typeClass == null) {
            return result;
        }

        var psiSubstitutor = genericType.getSubstitutor();

        for (PsiTypeParameter typeParameter : typeClass.getTypeParameters()) {
            var psiType = psiSubstitutor.substitute(typeParameter);

            if (psiType instanceof PsiClassReferenceType) {
                var psiClass = ((PsiClassReferenceType) psiType).resolve();
                if (psiClass != null) {
                    result.add(psiClass);
                }
            }
        }

        return result;
    }

    private static boolean ignoreClass(PsiClass psiClass) {
        if (psiClass == null) {
            return true;
        }

        var fullClassName = psiClass.getQualifiedName();

        if (fullClassName == null) {
            return true;
        }

        // ignore jdk class
        if (fullClassName.startsWith("java")) {
            return true;
        }

        // ignore some log package
        if (fullClassName.startsWith("org.slf4j")
                || fullClassName.startsWith("org.jboss.logmanager")
                || fullClassName.startsWith("org.apache.log4j")
                || fullClassName.startsWith("ch.qos.logback")) {
            return true;
        }

        // todo should ignore some famous opensource dependency

        return false;
    }
}
