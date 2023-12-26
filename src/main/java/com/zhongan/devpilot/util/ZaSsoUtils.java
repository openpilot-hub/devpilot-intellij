package com.zhongan.devpilot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhongan.devpilot.enums.ZaSsoEnum;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.ide.BuiltInServerManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ZaSsoUtils {
    public static final String ZA_SSO_AUTH_URL = "https://nsso.zhonganonline.com/login?service=codeai&target=http://127.0.0.1:%s/za/sso/callback";

    public static final String ZA_TI_SSO_AUTH_URL = "https://za-uc.in.za/login?service=codeai&target=http://127.0.0.1:%s/za/sso/callback";

    private static final String baseUrl = "http://openapi-cloud-pub.zhonganinfo.com/openpilot-hub";

    private static final String zaUserInfoUrl = baseUrl + "/za/user?ticket=";

    private static final String zaTiUserInfoUrl = baseUrl + "/zati/user?ticket=";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getZaSsoAuthUrl(ZaSsoEnum ssoEnum) {
        var port = BuiltInServerManager.getInstance().getPort();

        switch (ssoEnum) {
            case ZA_TI:
                return String.format(ZA_TI_SSO_AUTH_URL, port);
            case ZA:
            default:
                return String.format(ZA_SSO_AUTH_URL, port);
        }
    }

    public static ZaUser zaSsoAuth(ZaSsoEnum zaSsoEnum, String ticket) {
        String url = getUserInfoUrl(zaSsoEnum) + ticket;

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
                return objectMapper.readValue(result, ZaUser.class);
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

    private static String getUserInfoUrl(ZaSsoEnum zaSsoEnum) {
        switch (zaSsoEnum) {
            case ZA_TI:
                return zaTiUserInfoUrl;
            case ZA:
            default:
                return zaUserInfoUrl;
        }
    }

    public static boolean isLogin(ZaSsoEnum zaSsoEnum) {
        var settings = AIGatewaySettingsState.getInstance();
        switch (zaSsoEnum) {
            case ZA_TI:
                return StringUtils.isNotBlank(settings.getTiSsoToken()) && StringUtils.isNotBlank(settings.getTiSsoUsername());
            case ZA:
            default:
                return StringUtils.isNotBlank(settings.getSsoToken()) && StringUtils.isNotBlank(settings.getSsoUsername());
        }
    }

    public static void login(ZaSsoEnum zaSsoEnum, String token, String username) {
        var settings = AIGatewaySettingsState.getInstance();
        switch (zaSsoEnum) {
            case ZA_TI:
                settings.setTiSsoToken(token);
                settings.setTiSsoUsername(username);
                break;
            case ZA:
            default:
                settings.setSsoToken(token);
                settings.setSsoUsername(username);
                break;
        }
    }

    public static String zaSsoUsername(ZaSsoEnum zaSsoEnum) {
        var settings = AIGatewaySettingsState.getInstance();
        switch (zaSsoEnum) {
            case ZA_TI:
                return settings.getTiSsoUsername();
            case ZA:
            default:
                return settings.getSsoUsername();
        }
    }

    public static void logout(ZaSsoEnum zaSsoEnum) {
        var settings = AIGatewaySettingsState.getInstance();
        switch (zaSsoEnum) {
            case ZA_TI:
                settings.setTiSsoUsername(null);
                settings.setTiSsoToken(null);
                break;
            case ZA:
            default:
                settings.setSsoUsername(null);
                settings.setSsoToken(null);
                break;
        }
    }

    public static String getSsoType() {
        var settings = AIGatewaySettingsState.getInstance();
        return settings.getSelectedSso().toLowerCase(Locale.ROOT);
    }

    public static ZaSsoEnum getSsoEnum() {
        var settings = AIGatewaySettingsState.getInstance();
        return ZaSsoEnum.fromName(settings.getSelectedSso());
    }

    public static class ZaUser {
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
