package com.zhongan.devpilot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongan.devpilot.settings.state.TrialServiceSettingsState;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.ide.BuiltInServerManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class GithubAuthUtils {
    public static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize?client_id=050d14714ba54e734546&redirect_uri=http://127.0.0.1:%s/github/callback";

    private static final String baseUrl = "https://devpilot.zhongan.com/hub";

    private static final String userInfoUrl = baseUrl + "/github/user?code=";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getGithubAuthUrl() {
        return String.format(GITHUB_AUTH_URL, BuiltInServerManager.getInstance().getPort());
    }

    public static GithubUser githubAuth(String code) {
        String url = userInfoUrl + code;

        okhttp3.Response response = null;

        try {
            var request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            var call = client.newCall(request);
            response = call.execute();

            if (response.isSuccessful()) {
                var result = Objects.requireNonNull(response.body()).string();
                return objectMapper.readValue(result, GithubUser.class);
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return null;
    }

    public static boolean isLogin() {
        var settings = TrialServiceSettingsState.getInstance();
        return StringUtils.isNotBlank(settings.getGithubToken())
                && StringUtils.isNotBlank(settings.getGithubUsername())
                && settings.getGithubUserId() != null;
    }

    public static void logout() {
        var setting = TrialServiceSettingsState.getInstance();
        setting.setGithubToken(null);
        setting.setGithubUsername(null);
        setting.setGithubUserId(null);
    }

    public static void login(String username, String token, Long userId) {
        var setting = TrialServiceSettingsState.getInstance();
        setting.setGithubToken(token);
        setting.setGithubUsername(username);
        setting.setGithubUserId(userId);
    }

    public static void login(GithubUser githubUser) {
        var setting = TrialServiceSettingsState.getInstance();
        setting.setGithubToken(githubUser.getToken());
        setting.setGithubUsername(githubUser.getUsername());
        setting.setGithubUserId(githubUser.getId());
    }

    public static class GithubUser {
        private Long id;

        private String username;

        private String email;

        private Long companyId;

        private String companyName;

        private Long departmentId;

        private String userType;

        private String token;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public void setCompanyId(Long companyId) {
            this.companyId = companyId;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
