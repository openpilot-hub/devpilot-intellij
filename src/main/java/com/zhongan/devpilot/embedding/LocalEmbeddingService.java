package com.zhongan.devpilot.embedding;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.zhongan.devpilot.agents.BinaryManager;
import com.zhongan.devpilot.embedding.background.EmbeddingBackground;
import com.zhongan.devpilot.embedding.entity.DevPilotFileInfo;
import com.zhongan.devpilot.embedding.entity.index.IndexFileDiff;
import com.zhongan.devpilot.embedding.entity.index.IndexedFile;
import com.zhongan.devpilot.embedding.entity.index.LocalIndex;
import com.zhongan.devpilot.embedding.entity.java.file.JavaFileMeta;
import com.zhongan.devpilot.embedding.entity.request.EmbeddingChunkRequest;
import com.zhongan.devpilot.embedding.entity.request.EmbeddingDeleteRequest;
import com.zhongan.devpilot.embedding.entity.request.VectorIndexRequest;
import com.zhongan.devpilot.embedding.enums.DevPilotFileType;
import com.zhongan.devpilot.embedding.scanner.IgnoreAwareScanner;
import com.zhongan.devpilot.integrations.llms.LlmProviderFactory;
import com.zhongan.devpilot.provider.file.FileAnalyzeProviderFactory;
import com.zhongan.devpilot.util.GitUtil;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.MD5Utils;
import com.zhongan.devpilot.util.ProjectUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class LocalEmbeddingService {
    private static final Logger log = Logger.getInstance(LocalEmbeddingService.class);

    // 0 - doing, 1 - done
    private static final Map<String, Integer> indexStatusMap = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final int batchFileNum = 20;

    private static final int maxIndexFileNum = 1000;

    /**
     * Starts thread processing interval 15 minutes
     */
    public static void start(Project project) {
//        scheduler.scheduleWithFixedDelay(() -> {
//            var enabled = LocalRagSettingsState.getInstance().getEnable();
//
//            // only the setting enabled will process the project index
//            if (enabled) {
//                wrapIndexTask(project, LocalEmbeddingService::indexProject);
//            }
//        }, 15L, 15L, TimeUnit.MINUTES);
    }

    public static void immediateStart(Project project) {
        new EmbeddingBackground(project).queue();
    }

    public static void immediateStartCurrentProject() {
        var project = ProjectManager.getInstance().getOpenProjects()[0];
        immediateStart(project);
    }

    public static void indexProject(Project project) {
        if (!LoginUtils.isLogin()) {
            return;
        }
        var indexJsonFile = getIndexJson(project);
        if (indexJsonFile == null) {
            // skip if index file not exists
            return;
        }

        var oldIndex = JsonUtils.fromJson(indexJsonFile, LocalIndex.class);
        var newIndex = buildFullIndex(project);

        if (newIndex.getIndexedFiles() != null && newIndex.getIndexedFiles().size() > maxIndexFileNum) {
            log.warn("Index file size too large, skip index");
            return;
        }

        // write to local index file
        boolean result = JsonUtils.toJson(indexJsonFile, newIndex);
        if (!result) {
            log.warn("Index write to file failed");
        }

        IndexFileDiff diff = null;

        if (oldIndex != null) {
            // not first index, should calculate index diff
            diff = fileDiff(oldIndex, newIndex);
        }

        // if first index, just upload all files
        batchUploadIndex(project, newIndex, diff);
    }

    public static void wrapIndexTask(Project project, Consumer<Project> consumer) {
        Project currentProject = ProjectUtil.getCurrentContextProject();
        if (project != currentProject) {
            return;
        }
        Integer status = indexStatusMap.get(project.getBasePath());
        if (status != null && status == 0) {
            // skip if index is running
            return;
        }

        indexStatusMap.put(project.getBasePath(), 0);
        try {
            consumer.accept(project);
        } catch (Throwable e) {
            log.warn("Index task failed", e);
        }
        indexStatusMap.put(project.getBasePath(), 1);
    }

    private static File getIndexJson(Project project) {
        // read local index
        var homeDir = BinaryManager.INSTANCE.getHomeDir();
        if (homeDir == null) {
            log.warn("Home dir is null, skip building local index.");
            return null;
        }

        var indexDir = Paths.get(homeDir.getAbsolutePath(), "index").toFile();
        if (!indexDir.exists() && !indexDir.mkdir()) {
            log.warn("Cannot create index directory.");
            return null;
        }

        var projectName = getProjectName(project);
        if (projectName == null) {
            log.warn("Cannot found project path.");
            return null;
        }

        var projectIndex = Paths.get(indexDir.getAbsolutePath(), projectName).toFile();
        if (!projectIndex.exists() && !projectIndex.mkdir()) {
            log.warn("Cannot create project index directory.");
            return null;
        }

        var projectIndexJson = Paths.get(projectIndex.getAbsolutePath(), "index.json").toFile();
        try {
            if (!projectIndexJson.exists() && !projectIndexJson.createNewFile()) {
                log.warn("Cannot create project index json file.");
                return null;
            }
        } catch (IOException e) {
            log.warn("Create project index json file failed." + e.getMessage());
            return null;
        }

        return projectIndexJson;
    }

    public static String getProjectName(Project project) {
        var path = project.getBasePath();
        if (path == null) {
            return null;
        }
        return path.replace("/", "_");
    }

    private static String getRelativePath(Project project, String path) {
        return StringUtils.replace(path, project.getBasePath() + File.separator, "");
    }

    private static LocalIndex buildFullIndex(Project project) {
        LocalIndex localIndex = new LocalIndex();
        localIndex.setProjectName(getProjectName(project));
        localIndex.setGitRepo(GitUtil.getRepoUrlFromFile(project, getVirtualFile(project.getBasePath())));

        List<DevPilotFileType> list = new ArrayList<>();
        list.add(DevPilotFileType.POM);
        list.add(DevPilotFileType.JAVA);
        IgnoreAwareScanner scanner = new IgnoreAwareScanner(project.getBasePath(), list);
        scanner.scan();
        Map<DevPilotFileType, List<File>> typeFiles = scanner.getFiles();

        List<IndexedFile> indexedFiles = new ArrayList<>();

        for (DevPilotFileType item : typeFiles.keySet()) {
            for (File file : typeFiles.get(item)) {
                IndexedFile indexedFile = new IndexedFile();
                indexedFile.setFileName(file.getName());
                indexedFile.setAbsolutePath(file.getAbsolutePath());
                indexedFile.setFilePath(getRelativePath(project, file.getAbsolutePath()));
                indexedFile.setFileType(item.getExtension());
                indexedFile.setFileSize(file.length());
                indexedFile.setFileHash(MD5Utils.calculateMD5(getVirtualFile(file)));
                indexedFiles.add(indexedFile);
            }
        }

        localIndex.setIndexedFiles(indexedFiles);
        return localIndex;
    }

    private static IndexFileDiff fileDiff(LocalIndex oldIndex, LocalIndex newIndex) {
        var diff = new IndexFileDiff();

        if (oldIndex == null || newIndex == null) {
            return diff;
        }

        diff.setProjectName(oldIndex.getProjectName());
        diff.setGitRepo(newIndex.getGitRepo());

        var oldFiles = oldIndex.getIndexedFiles();
        var newFiles = newIndex.getIndexedFiles();

        var oldFileMap = oldFiles.stream().
                collect(Collectors.toMap(IndexedFile::getFilePath, file -> file));
        var newFileMap = newFiles.stream().
                collect(Collectors.toMap(IndexedFile::getFilePath, file -> file));

        diff.setAddedFileList(newFiles.stream()
                .filter(file -> !oldFileMap.containsKey(file.getFilePath()))
                .collect(Collectors.toList()));

        diff.setDeletedFileList(oldFiles.stream()
                .filter(file -> !newFileMap.containsKey(file.getFilePath()))
                .collect(Collectors.toList()));

        diff.setModifiedFileList(newFiles.stream()
                .filter(newFile -> {
                    var oldFile = oldFileMap.get(newFile.getFilePath());
                    return oldFile != null && !StringUtils.equals(oldFile.getFileHash(), newFile.getFileHash());
                })
                .collect(Collectors.toList()));

        return diff;
    }

    private static void batchUploadIndex(Project project, LocalIndex index, IndexFileDiff diff) {
        var fileList = index.getIndexedFiles();
        var llmProvider = LlmProviderFactory.INSTANCE.getLlmProvider(project);

        // first handle delete file
        if (diff != null && !CollectionUtils.isEmpty(diff.getDeletedFileList())) {
            var deleteRequest = new EmbeddingDeleteRequest();
            deleteRequest.setHomeDir(BinaryManager.INSTANCE.getHomeDir().getAbsolutePath());
            deleteRequest.setProjectName(project.getBasePath());
            deleteRequest.setGitRepo(index.getGitRepo());

            List<String> deleteFilePathList = diff.getDeletedFileList()
                    .stream().map(IndexedFile::getFilePath).collect(Collectors.toList());

            deleteRequest.setDeletedFiles(deleteFilePathList);

            var deleteResponse = llmProvider.submitDelete(project, deleteRequest);
            if (deleteResponse == null) {
                log.warn("delete chunk failed");
            }
        }

        var chunkRequest = new EmbeddingChunkRequest();
        chunkRequest.setHomeDir(BinaryManager.INSTANCE.getHomeDir().getAbsolutePath());
        chunkRequest.setProjectName(project.getBasePath());
        chunkRequest.setProjectLocation(project.getBasePath());
        chunkRequest.setGitRepo(index.getGitRepo());

        var taskList = transformIndexToDevPilotFile(project, fileList);

        // batch task, 20 files per batch
        for (int i = 0; i < taskList.size(); i += batchFileNum) {
            chunkRequest.setBatchId(UUID.randomUUID().toString());
            var indexNum = Math.min(i + batchFileNum, taskList.size());
            var fileInfoList = taskList.subList(i, indexNum);
            var fileMap = calculateVectorIndex(fileInfoList);
            chunkRequest.setChangedRecords(fileMap);
            chunkRequest.setSubmitEnd(indexNum == taskList.size());

            var response = llmProvider.submitChunk(project, chunkRequest);
            if (response == null) {
                log.warn("submit chunk failed");
            }
            if (response != null && response.isNeedAbortSubmit()) {
                log.warn("Submit chunk failed, need abort submit for not login");
                return;
            }
        }
    }

    private static List<DevPilotFileInfo> transformIndexToDevPilotFile(Project project, List<IndexedFile> fileList) {
        var taskList = new ArrayList<DevPilotFileInfo>();

        for (var file : fileList) {
            ApplicationManager.getApplication().runReadAction(() -> {
                var info = FileAnalyzeProviderFactory
                        .getProvider(file.getFileType()).parseFile(project, getVirtualFile(file.getAbsolutePath()));
                if (null != info) {
                    taskList.add(info);
                }
            });
        }

        return taskList;
    }

    private static Map<String, List<VectorIndexRequest>> calculateVectorIndex(List<DevPilotFileInfo> fileInfoList) {
        var result = new HashMap<String, List<VectorIndexRequest>>();

        for (var fileInfo : fileInfoList) {
            try {
                if (CollectionUtils.isEmpty(fileInfo.getFileMeta().getFunctionMetas())) {
                    continue;
                }
                var vectorIndexRequests = new ArrayList<VectorIndexRequest>();
                var fileMeta = fileInfo.getFileMeta();
                if (fileMeta instanceof JavaFileMeta) {
                    ((JavaFileMeta) fileMeta).setLlmSummary(((JavaFileMeta) fileMeta).getClazzDef());
                    ((JavaFileMeta) fileMeta).setChunkHash(MD5Utils.calculateMD5(((JavaFileMeta) fileMeta).getClazzDef().getBytes(StandardCharsets.UTF_8)));
                    vectorIndexRequests.add(VectorIndexRequest.from(fileInfo, (JavaFileMeta) fileMeta));
                }
                fileInfo.getFileMeta().getFunctionMetas().forEach(functionMeta -> {
                    var content = StringUtils.join(
                            StringUtils.defaultIfEmpty(functionMeta.getComments(), StringUtils.EMPTY),
                            StringUtils.defaultIfEmpty(functionMeta.getContent(), StringUtils.EMPTY));

                    functionMeta.setChunkHash(MD5Utils.calculateMD5(content.getBytes(StandardCharsets.UTF_8)));
                    functionMeta.setFunctionLLMSummary(content);
                    vectorIndexRequests.add(VectorIndexRequest.from(fileInfo, functionMeta));
                });

                result.put(fileInfo.getFilePath(), vectorIndexRequests);
                log.info("----success:----" + fileInfo.getFilePath());
            } catch (Exception e) {
                log.error("Error occurred while parsing FileInfo.", e);
            }
        }

        return result;
    }

    private static VirtualFile getVirtualFile(File file) {
        String url = VirtualFileManager.constructUrl("file", file.getAbsolutePath());
        return VirtualFileManager.getInstance().findFileByUrl(url);
    }

    private static VirtualFile getVirtualFile(String path) {
        String url = VirtualFileManager.constructUrl("file", path);
        return VirtualFileManager.getInstance().findFileByUrl(url);
    }
}
