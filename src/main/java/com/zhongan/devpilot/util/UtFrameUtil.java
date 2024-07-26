package com.zhongan.devpilot.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.zhongan.devpilot.enums.UtFrameTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import static com.zhongan.devpilot.enums.UtFrameTypeEnum.JUNIT4_MOCKITO;
import static com.zhongan.devpilot.enums.UtFrameTypeEnum.JUNIT4_POWERMOCK;
import static com.zhongan.devpilot.enums.UtFrameTypeEnum.JUNIT5_MOCKITO;
import static com.zhongan.devpilot.enums.UtFrameTypeEnum.JUNIT5_POWERMOCK;
import static org.jetbrains.idea.maven.model.MavenArtifactState.CONFLICT;
import static org.jetbrains.idea.maven.model.MavenArtifactState.DUPLICATE;
import static org.jetbrains.idea.maven.model.MavenArtifactState.EXCLUDED;

public class UtFrameUtil {

    private static final String POWER_MOCK_GROUP_ID = "org.powermock";

    private static final String MOCKITO_GROUP_ID = "org.mockito";

    private static final String JUNIT5_GROUP_ID = "org.junit.jupiter";

    private static final String JUNIT4_GROUP_ID = "junit";

    private UtFrameUtil() {
    }

    public static UtFrameTypeEnum getUTFrameWork(Project project, Editor editor) {
        try {
            if (project == null || project.getBasePath() == null) {
                return JUNIT5_MOCKITO;
            }
            PsiFile pomXmlFile = findPomXmlFile(project, editor);
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(pomXmlFile.getProject());
            MavenProject mavenProject = mavenProjectsManager.findProject(pomXmlFile.getVirtualFile());
            if (mavenProject == null) {
                return JUNIT5_MOCKITO;
            }
            List<MavenArtifactNode> dependencyTree = mavenProject.getDependencyTree();
            Map<String, List<MavenArtifactNode>> testArtifactsMap = createTestArtifactsMap(dependencyTree);
            List<Map.Entry<String, List<MavenArtifactNode>>> testDependencies = new ArrayList<>(testArtifactsMap.entrySet());
            Map.Entry<String, List<MavenArtifactNode>> junit5 = testDependencies.stream().filter(d -> d.getKey().startsWith(JUNIT5_GROUP_ID)).findAny().orElse(null);
            Map.Entry<String, List<MavenArtifactNode>> mockito = testDependencies.stream().filter(d -> d.getKey().startsWith(MOCKITO_GROUP_ID)).findAny().orElse(null);
            if (junit5 != null && mockito != null) {
                return JUNIT5_MOCKITO;
            }
            Map.Entry<String, List<MavenArtifactNode>> junit4 = testDependencies.stream().filter(d -> d.getKey().startsWith(JUNIT4_GROUP_ID)).findAny().orElse(null);
            Map.Entry<String, List<MavenArtifactNode>> powerMock = testDependencies.stream().filter(d -> d.getKey().startsWith(POWER_MOCK_GROUP_ID)).findAny().orElse(null);
            if (junit4 != null && powerMock != null) {
                return JUNIT4_POWERMOCK;
            } else if (junit5 != null && powerMock != null) {
                return JUNIT5_POWERMOCK;
            } else if (junit4 != null && mockito != null) {
                return JUNIT4_MOCKITO;
            }
            return JUNIT5_MOCKITO;
        } catch (Exception e) {
            return JUNIT5_MOCKITO;
        }
    }

    private static Map<String, List<MavenArtifactNode>> createTestArtifactsMap(List<MavenArtifactNode> dependencyTree) {
        final Map<String, List<MavenArtifactNode>> map = new TreeMap<>();
        addAll(map, dependencyTree, 0);
        return map;
    }

    private static void addAll(Map<String, List<MavenArtifactNode>> map, List<MavenArtifactNode> artifactNodes, int i) {
        if (map == null) {
            return;
        }
        for (MavenArtifactNode mavenArtifactNode : artifactNodes) {
            final MavenArtifact artifact = mavenArtifactNode.getArtifact();
            final String key = getArtifactKey(artifact);
            final List<MavenArtifactNode> mavenArtifactNodes = map.get(key);
            if (!"test".equals(mavenArtifactNode.getOriginalScope())) {
                continue;
            }
            if (mavenArtifactNode.getState().equals(DUPLICATE) || mavenArtifactNode.getState().equals(EXCLUDED) || mavenArtifactNode.getState().equals(CONFLICT)) {
                continue;
            }

            if (mavenArtifactNodes == null) {
                final ArrayList<MavenArtifactNode> value = new ArrayList<>(1);
                value.add(mavenArtifactNode);
                map.put(key, value);
            } else {
                mavenArtifactNodes.add(mavenArtifactNode);
            }
            addAll(map, mavenArtifactNode.getDependencies(), i + 1);
        }
    }

    @NotNull
    private static String getArtifactKey(MavenArtifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }

    public static PsiFile findPomXmlFile(Project project, Editor editor) {
        Document document = editor.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null) {
            return null;
        }
        PsiDirectory psiDirectory = psiFile.getContainingDirectory();
        if (psiDirectory == null) {
            return null;
        }
        while (psiDirectory != null) {
            PsiFile[] psiFiles = psiDirectory.getFiles();
            for (PsiFile file : psiFiles) {
                if (file instanceof XmlFile && "pom.xml".equals(file.getName())) {
                    return file;
                }
            }
            psiDirectory = psiDirectory.getParentDirectory();
        }
        return null;
    }

}