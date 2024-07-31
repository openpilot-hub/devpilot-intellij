package com.zhongan.devpilot.util;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;

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
        var parameterClass = getMethodParameterTypeClass(element);
        var returnClass = getMethodReturnTypeClass(element);

        if (parameterClass == null) {
            return returnClass;
        }

        if (returnClass == null) {
            return parameterClass;
        }

        return parameterClass + returnClass;
    }

    public static String getMethodReturnTypeClass(@NotNull PsiElement element) {
        String result = null;

        if (element instanceof PsiMethod) {
            var returnType = ((PsiMethod) element).getReturnType();

            if (returnType instanceof PsiClassReferenceType) {
                var referenceType = (PsiClassReferenceType) returnType;
                var returnTypeClass = referenceType.resolve();
                result = getGenericType(referenceType);
                if (returnTypeClass != null && !ignoreClass(returnTypeClass)) {
                    return result + returnTypeClass.getText();
                }
            }
        }

        return result;
    }

    public static String getMethodParameterTypeClass(@NotNull PsiElement element) {
        var sb = new StringBuilder();

        if (element instanceof PsiMethod) {
            var params = ((PsiMethod) element).getParameterList().getParameters();

            for (JvmParameter parameter : params) {
                if (parameter.getType() instanceof PsiClassReferenceType) {
                    var referenceType = (PsiClassReferenceType) parameter.getType();
                    var psiClass = referenceType.resolve();
                    var genericClass = getGenericType(referenceType);
                    if (psiClass != null && !ignoreClass(psiClass)) {
                        sb.append(psiClass.getText()).append("\n");
                    }
                    sb.append(genericClass);
                }
            }
        }

        if (sb.length() <= 0) {
            return null;
        }

        return sb.toString();
    }

    private static String getGenericType(PsiClassReferenceType referenceType) {
        var sb = new StringBuilder();

        var genericType = referenceType.resolveGenerics();
        var typeClass = genericType.getElement();

        if (typeClass == null) {
            return "";
        }

        var psiSubstitutor = genericType.getSubstitutor();

        for (PsiTypeParameter typeParameter : typeClass.getTypeParameters()) {
            var psiType = psiSubstitutor.substitute(typeParameter);

            if (psiType instanceof PsiClassReferenceType) {
                var psiClass = ((PsiClassReferenceType) psiType).resolve();
                if (psiClass != null && !ignoreClass(psiClass)) {
                    sb.append(psiClass.getText()).append("\n");
                }
            }
        }

        return sb.toString();
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

        // todo should ignore some famous opensource dependency

        return false;
    }
}
