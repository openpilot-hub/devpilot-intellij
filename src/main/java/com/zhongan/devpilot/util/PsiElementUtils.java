package com.zhongan.devpilot.util;

import com.intellij.lang.jvm.JvmParameter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PropertyUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
                PsiClass psiClass = (PsiClass) element;
                if (ignoreClass(psiClass)) {
                    continue;
                }
                result.append("Class: ").append(psiClass.getQualifiedName()).append("\n\n");
            }

            if (element instanceof PsiMethod) {
                PsiMethod psiMethod = (PsiMethod) element;
                if (ignoreMethod(psiMethod)) {
                    continue;
                }
                if (psiMethod.getContainingClass() != null) {
                    result.append("Method: ").append(StringUtils.join(psiMethod.getContainingClass().getQualifiedName(), psiMethod.getName(), "#")).append("\n");
                } else {
                    result.append("Method: ").append("\n");
                }
            }

            result.append(element.getText()).append("\n\n");
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

    public static String getImportInfo(PsiFile psiFile) {
        StringBuilder importedClasses = new StringBuilder();
        if (!(psiFile instanceof PsiJavaFile)) {
            return "";
        }
        PsiImportList importList = ((PsiJavaFile) psiFile).getImportList();
        if (importList != null) {
            PsiImportStatementBase[] importStatements = Arrays.stream(importList.getAllImportStatements()).toArray(PsiImportStatementBase[]::new);
            for (PsiImportStatementBase importStatement : importStatements) {
                importedClasses.append(importStatement.getText()).append(System.lineSeparator());
            }
        }
        return importedClasses.toString();
    }

    public static List<PsiElement> referenceRecall(Project project, List<String> refs) {
        if (CollectionUtils.isEmpty(refs)) {
            return Collections.emptyList();
        }
        List<String> finalRefs = removeDuplicates(refs);
        return doRecall(project, finalRefs);
    }

    private static List<String> removeDuplicates(List<String> refs) {
        if (CollectionUtils.isEmpty(refs)) {
            return Collections.emptyList();
        }
        Set<String> uniqueRefs = new HashSet<>(refs);
        HashSet<String> res = new LinkedHashSet<>(uniqueRefs);
        uniqueRefs.forEach(ref -> {
            if (StringUtils.contains(ref, "#")) {
                String[] split = StringUtils.split(ref, "#");
                String className = split[0];
                if (res.contains(className)) {
                    res.remove(ref);
                }
            }
        });
        return new ArrayList<>(res);
    }

    private static List<PsiElement> doRecall(Project project, List<String> references) {
        List<PsiElement> res = new ArrayList<>();
        if (CollectionUtils.isEmpty(references)) {
            return Collections.emptyList();
        }
        for (String ref : references) {
            if (StringUtils.contains(ref, "#")) {
                String[] split = StringUtils.split(ref, "#");
                String className = split[0];
                String methodName = split[1];
                res.add(methodRecall(project, className, methodName));
            } else {
                res.add(classRecall(project, ref));
            }
        }
        return res;
    }

    public static PsiElement classRecall(Project project, String clz) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(clz, GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return null;
        }
        if (isCompiled(psiClass)) {
            PsiClass sourceMirror = ((ClsClassImpl) psiClass).getSourceMirrorClass();
            if (sourceMirror != null) {
                psiClass = sourceMirror;
            }
            if (StringUtils.contains(psiClass.getText(), "/* compiled code */")) {
                return null;
            }
        }
        return psiClass;
    }

    private static PsiElement methodRecall(Project project, String clz, String methodName) {
        PsiClass[] classes = JavaPsiFacade.getInstance(project).findClasses(clz, GlobalSearchScope.allScope(project));
        if (classes.length == 0) {
            return null;
        }
        PsiClass psiClass = classes[0];
        if (psiClass.isInterface()) {
            PsiClass first = ClassInheritorsSearch.search(psiClass).findFirst();
            if (first != null) {
                psiClass = first;
            }
        }
        if (isCompiled(psiClass)) {
            PsiClass sourceMirror = ((ClsClassImpl) psiClass).getSourceMirrorClass();
            if (sourceMirror != null) {
                psiClass = sourceMirror;
            }
        }
        PsiMethod psiMethod = Arrays.stream(psiClass.findMethodsByName(methodName, true)).max(Comparator.comparingInt(o -> o.getParameters().length)).orElse(null);
        if (isValidMethod(psiMethod, psiClass)) {
            return psiMethod;
        }
        return null;
    }

    private static boolean isValidMethod(PsiMethod psiMethod, PsiClass psiClass) {
        if (psiMethod == null) return false;
        if (PropertyUtil.isSimpleGetter(psiMethod) || PropertyUtil.isSimpleSetter(psiMethod)) return false;
        if (isCompiled(psiClass)) {
            return !StringUtils.contains(psiMethod.getText(), "/* compiled code */");
        } else {
            return psiMethod.getBody() != null;
        }
    }

    private static boolean isCompiled(@NotNull PsiClass psiClass) {
        return psiClass instanceof ClsClassImpl;
    }

    /**
     * used in rag case if need.
     *
     * @param project
     * @param psiClass
     * @return
     */
    public static Pair<String, String> getJarTitleAndVersion(Project project, PsiClass psiClass) {
        PsiFile psiFile = psiClass.getContainingFile();
        VirtualFile virtualFile = psiFile.getVirtualFile();
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        String title = "", version = "";
        if (fileIndex.isInLibraryClasses(virtualFile)) {
            VirtualFile classRoot = fileIndex.getClassRootForFile(virtualFile);
            if (classRoot != null) {
                String jarPath = classRoot.getPresentableUrl();
                Manifest manifest;
                try (JarFile jarFile = new JarFile(jarPath)) {
                    manifest = jarFile.getManifest();
                } catch (Exception e) {
                    return Pair.of(title, version);
                }
                if (manifest != null) {
                    Attributes mainAttributes = manifest.getMainAttributes();
                    version = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
                    title = mainAttributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
                }
            }
        }
        return Pair.of(title, version);
    }

}
