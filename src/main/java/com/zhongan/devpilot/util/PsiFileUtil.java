package com.zhongan.devpilot.util;

import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;

import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiFileUtil {

    private static final Set<String> WEB_ANNOTATIONS = ImmutableSet.of(
            "Controller", "RestController"
    );

    public static boolean isCaretInWebClass(@NotNull Project project, @NotNull Editor editor) {
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile != null) {
            int offset = editor.getCaretModel().getOffset();
            PsiElement currentElement = psiFile.findElementAt(offset);
            while (currentElement != null) {
                if (currentElement instanceof PsiClass) {
                    return isWebClass(currentElement);
                }
                currentElement = currentElement.getParent();
            }
        }
        return false;
    }

    public static boolean isWebClass(@Nullable PsiElement element) {
        if (element instanceof PsiClass) {
            PsiClass psiClass = (PsiClass) element;
            PsiModifierList modifierList = psiClass.getModifierList();
            if (modifierList == null) {
                return false;
            }
            for (String springController : WEB_ANNOTATIONS) {
                if (modifierList.hasAnnotation(springController)) {
                    return true;
                }
            }
        }
        return false;
    }

}
