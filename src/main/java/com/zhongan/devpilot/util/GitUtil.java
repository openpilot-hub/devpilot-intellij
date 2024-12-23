package com.zhongan.devpilot.util;

import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.HashMap;
import java.util.Map;

import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;

import static com.zhongan.devpilot.constant.DefaultConst.RAG_DEFAULT_HOST;

public class GitUtil {

    private static final String ORIGIN = "origin";

    private static final String SLASH = "/";

    private static final String GIT = ".git";

    private static final Map<String, State> map = new HashMap<>();

    private static final long syncTimeInterval = 6 * 60 * 60 * 1000;

    private static final String queryAppCodeEmbeddedStateUrl = RAG_DEFAULT_HOST;

    public static String getRepoNameFromFile(Project project, VirtualFile virtualFile) {
        GitRepository repository = git4idea.GitUtil.getRepositoryManager(project).getRepositoryForFile(virtualFile);
        if (repository == null) {
            return null;
        }
        for (GitRemote remote : repository.getRemotes()) {
            if (remote.getName().equals(ORIGIN)) {
                String remoteUrl = remote.getFirstUrl();
                if (remoteUrl == null) {
                    return null;
                }

                int lastSlashIndex = remoteUrl.lastIndexOf(SLASH);
                int gitIndex = remoteUrl.lastIndexOf(GIT);

                if (lastSlashIndex == -1 || gitIndex == -1) {
                    return null;
                }

                return remoteUrl.substring(lastSlashIndex + 1, gitIndex);
            }
        }
        return null;
    }

    public static String getRepoUrlFromFile(Project project, VirtualFile virtualFile) {
        GitRepository repository = git4idea.GitUtil.getRepositoryManager(project).getRepositoryForFile(virtualFile);
        if (repository == null) {
            return null;
        }
        for (GitRemote remote : repository.getRemotes()) {
            if (remote.getName().equals(ORIGIN)) {
                return remote.getFirstUrl();
            }
        }
        return null;
    }

    public static Boolean isRepoEmbedded(String appName) {
        Boolean embedded = Boolean.FALSE;
        if (!map.containsKey(appName) || (System.currentTimeMillis() - map.get(appName).getTimestamp()) > syncTimeInterval) {
            // 调用接口
            Response response;
            try {
                Request request = new Request.Builder()
                        .url(queryAppCodeEmbeddedStateUrl + appName)
                        .header("User-Agent", UserAgentUtils.buildUserAgent())
                        .header("Auth-Type", LoginUtils.getLoginType())
                        .get()
                        .build();

                Call call = OkhttpUtils.getClient().newCall(request);
                response = call.execute();

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    RepoEmbedded repoEmbedded = new Gson().fromJson(responseBody, RepoEmbedded.class);
                    embedded = repoEmbedded.getEmbedded();
                }
            } catch (Exception e) {
                return Boolean.FALSE;
            }

            map.put(appName, new State(embedded, System.currentTimeMillis()));
        }
        return map.get(appName).getEmbedded();
    }

    public static class RepoEmbedded {

        private String repoName;

        private Boolean embedded;

        public Boolean getEmbedded() {
            return embedded;
        }

        public void setEmbedded(Boolean embedded) {
            this.embedded = embedded;
        }

        public String getRepoName() {
            return repoName;
        }

        public void setRepoName(String repoName) {
            this.repoName = repoName;
        }
    }

    public static class State {

        private Boolean embedded;

        private Long timestamp;

        public State(Boolean embedded, Long timestamp) {
            this.embedded = embedded;
            this.timestamp = timestamp;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public Boolean getEmbedded() {
            return embedded;
        }

        public void setEmbedded(Boolean embedded) {
            this.embedded = embedded;
        }
    }

}
