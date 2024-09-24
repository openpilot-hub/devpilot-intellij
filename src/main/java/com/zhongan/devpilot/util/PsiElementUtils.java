package com.zhongan.devpilot.util;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import java.util.ArrayList;
import java.util.Collection;
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

        return transformElementToString(classSet);
    }

    public static <T extends PsiElement> String transformElementToString(Collection<T> elements) {
        var result = new StringBuilder();

        for (T element : elements) {
            if (element instanceof PsiClass) {
                if (ignoreClass((PsiClass) element)) {
                    continue;
                }
            }

            if (element instanceof PsiMethod) {
                if (ignoreMethod((PsiMethod) element)) {
                    continue;
                }
            }

            result.append(element.getText()).append("\n");
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
                result.addAll(getTypeClassAndGenericType(referenceType));
                return result;
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
                    result.addAll(getTypeClassAndGenericType(referenceType));
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
                result.addAll(getTypeClassAndGenericType(referenceType));
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

    private static List<PsiClass> getTypeClassAndGenericType(PsiClassReferenceType referenceType) {
        var result = new ArrayList<PsiClass>();

        var psiClass = referenceType.resolve();
        if (psiClass != null) {
            result.add(psiClass);
        }
        result.addAll(getGenericType(referenceType));

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

    private static boolean ignoreMethod(PsiMethod psiMethod) {
        var psiClass = psiMethod.getContainingClass();
        return ignoreClass(psiClass);
    }

    public static Set<PsiElement> parseElementsList(Project project, List<String> elements) {
        var result = new HashSet<PsiElement>();

        for (String element : elements) {
            // element format class#method
            var arrays = element.split("#");
            var classFullName = arrays[0];
            String methodName = null;

            if (arrays.length > 1) {
                methodName = arrays[1];
            }

            var e = getElementByName(project, classFullName, methodName);
            if (e != null) {
                result.add(e);
            }
        }

        return result;
    }

    private static PsiElement getElementByName(Project project, String className, String methodName) {
        var psiClass = findRealClass(project, className);

        if (psiClass != null) {
            if (methodName == null) {
                return psiClass;
            } else {
                var methods = psiClass.getMethods();
                for (var method : methods) {
                    if (methodName.equals(method.getName())) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    // AI model may confuse between inner class and normal class, so we should resolve this situation
    private static PsiClass findRealClass(Project project, String className) {
        var javaPsiFacade = JavaPsiFacade.getInstance(project);
        var factory = javaPsiFacade.getElementFactory();

        var classType = factory.createTypeByFQClassName(className);
        var psiClass = classType.resolve();

        if (psiClass != null) {
            return psiClass;
        }

        if (className.contains("$")) {
            className = className.replace('$', '.');
        } else {
            var lastDot = className.lastIndexOf('.');
            if (lastDot != -1) {
                className = className.substring(0, lastDot) + "$" + className.substring(lastDot + 1);
            }
        }

        classType = factory.createTypeByFQClassName(className);
        return classType.resolve();
    }

    public static PsiJavaFile getPsiJavaFileByFilePath(Project project, String filePath) {
        var virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (virtualFile != null) {
            var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (!(psiFile instanceof PsiJavaFile)) {
                return null;
            }
            return (PsiJavaFile) psiFile;
        }
        return null;
    }

    private static PsiClass getPsiClassByFile(PsiJavaFile psiJavaFile) {
        var classes = psiJavaFile.getClasses();
        if (classes.length > 0) {
            return classes[0];
        }
        return null;
    }

    public static String getImportList(PsiJavaFile psiJavaFile) {
        var importList = new StringBuilder();
        var importStatements = psiJavaFile.getImportList();
        if (importStatements != null) {
            var imports = importStatements.getImportStatements();
            for (PsiImportStatement importStatement : imports) {
                importList.append(importStatement.getQualifiedName()).append(";");
            }

            for (PsiImportStaticStatement importStatement : importStatements.getImportStaticStatements()) {
                if (importStatement.getImportReference() != null) {
                    importList.append(importStatement.getImportReference().getQualifiedName()).append(";");
                }
            }
        }
        return importList.toString();
    }

    public static String getFieldList(PsiJavaFile psiJavaFile) {
        var fieldList = new StringBuilder();

        var psiClass = getPsiClassByFile(psiJavaFile);
        if (psiClass == null) {
            return "";
        }

        var fields = psiClass.getFields();
        for (PsiField field : fields) {
            fieldList.append(field.getText()).append(System.lineSeparator());
        }
        return fieldList.toString();
    }
}
