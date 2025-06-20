package com.zhongan.devpilot.agents;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.agents.model.AgentInfoResp;
import com.zhongan.devpilot.settings.state.PersonalAdvancedSettingsState;
import com.zhongan.devpilot.util.JsonUtils;
import com.zhongan.devpilot.util.LoginUtils;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.ProcessUtils;
import com.zhongan.devpilot.util.ProjectUtil;
import com.zhongan.devpilot.util.UserAgentUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.zhongan.devpilot.constant.DefaultConst.TELEMETRY_HOST;

public class BinaryManager {
    private static final Logger LOG = Logger.getInstance(BinaryManager.class);

    public static final BinaryManager INSTANCE = new BinaryManager();

    private static final String COMPATIBLE_ARCH;

    private static final Map<String, String> IDE_INFO_MAP = new HashMap<>();

    public static final String EXECUTABLE_NAME = "devpilot-agents";

    public volatile AtomicBoolean reStarting = new AtomicBoolean(false);

    private final Map<String, String> bundledMD5Cache = new ConcurrentHashMap<>();

    private static final int BUFFER_SIZE = 8192;

    private static final String AARCH64 = "aarch64";

    private static final String ARCH_ = "_";

    private static final String X86_64 = "x86_64";

    private static final String WINDOWS_PLATFORM = "windows";

    private static final String MAC_PLATFORM = "darwin";

    private static final String LINUX_PLATFORM = "linux";

    private static final String BUNDLED_VERSION = "3.0.2";

    static {
        COMPATIBLE_ARCH = String.format("%s_%s", getSystemArch(), getPlatformName());
        IDE_INFO_MAP.put("type", DevPilotVersion.getVersionName().replace(" ", "_"));
    }

    public String getIdeInfoPath() {
        return "." + getIdeType() + "info";
    }

    public String getVersion() {
        return IDE_INFO_MAP.get("version");
    }

    public String getCompatibleArch() {
        return COMPATIBLE_ARCH;
    }

    @Contract(pure = true)
    private BinaryManager() {
        bundledMD5Cache.put(AARCH64.concat(ARCH_).concat(LINUX_PLATFORM), "cfec545d08a39a20a9db35f64703717b");
        bundledMD5Cache.put(X86_64.concat(ARCH_).concat(LINUX_PLATFORM), "29cb500570bb9565d80a508408546e51");
        bundledMD5Cache.put(AARCH64.concat(ARCH_).concat(MAC_PLATFORM), "19de1b929938e4e6dc95b0e4a7dc7bec");
        bundledMD5Cache.put(X86_64.concat(ARCH_).concat(MAC_PLATFORM), "ae5f89cb723af940e9086c9d1d0596be");
        bundledMD5Cache.put(AARCH64.concat(ARCH_).concat(WINDOWS_PLATFORM), "ae362f1ee59561aaf0a2a71420a068c9");
        bundledMD5Cache.put(X86_64.concat(ARCH_).concat(WINDOWS_PLATFORM), "d535382f1b521b16ab626d592a65b004");
        bundledMD5Cache.put("zip", "ac8871f4c26d49bc02481a44cbedce59");
    }

    public boolean shouldStartAgent() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        return openProjects.length != 0;
    }

    public boolean currentPortAvailable() {
        Pair<Integer, Long> integerLongPair = retrieveAlivePort();
        if (integerLongPair == null) {
            return false;
        }
        return agentAvailable(integerLongPair.first);
    }

    public Path getDefaultHomePath() {
        return Paths.get(getUserHome(), ".DevPilot");
    }

    public File getHomeDir() {
        File homeDir = getHomeDirectory().toFile();
        if (!homeDir.exists() && !homeDir.mkdirs()) {
            LOG.warn("Failed to create parent home directory." + homeDir.getName());
            return null;
        } else {
            File finalHomeDir = new File(homeDir, getIdeType());
            if (!finalHomeDir.exists() && !finalHomeDir.mkdirs()) {
                LOG.warn("Failed to create final home directory." + finalHomeDir.getName());
                return null;
            }

            boolean fromSources = ProjectUtil.isSandboxProject();
            if (fromSources) {
                finalHomeDir = new File(finalHomeDir, "sandbox");
                if (!finalHomeDir.exists() && !finalHomeDir.mkdirs()) {
                    LOG.warn("Failed to create final home directory for sandbox." + finalHomeDir.getName());
                    return null;
                }
            }
            return finalHomeDir;
        }
    }

    public String getBinaryPath(@NotNull File workDirectory) {
        File root = getBinaryRoot(workDirectory);
        if (root == null) {
            return null;
        } else {
            return getDefaultBinaryPath(root);
        }
    }

    public synchronized boolean postProcessBeforeRunning(File homeDir) {
        File binaryRoot = getBinaryRoot(homeDir);
        if (null == binaryRoot) {
            LOG.warn("Exception occurred while creating binary root.");
            return false;
        }
        boolean initRes = initBinary(binaryRoot);
        if (!initRes) {
            LOG.warn("Exception occurred while init binary.");
            return false;
        }
        return true;
    }

    public synchronized AgentCheckResult checkIfAgentRunning(File homeDir) {
        Pair<Integer, Long> infoPair = readProcessInfoFile(homeDir);
        if (infoPair != null) {
            if (agentAvailable(infoPair.first)) {
                LOG.info(String.format("Finding agent is running on port: [%s], pid: [%s]", infoPair.first, infoPair.second));
                return new AgentCheckResult(true, infoPair.second, infoPair.first);
            }
        }
        return new AgentCheckResult(false, null, null);
    }

    private boolean agentAvailable(Integer port) {
        if (port == null) {
            return false;
        }
        boolean isRunning;
        // Create a URL connection to the health endpoint
        try {
            Request request = new Request.Builder().url("http://localhost:" + port + "/health").get().build();
            Call call = OkhttpUtils.getClient().newCall(request);
            try (Response response = call.execute()) {
                isRunning = response.isSuccessful();
            }
        } catch (Exception e) {
            LOG.warn("Failed to check agent health", e);
            return false;
        }
        return isRunning;
    }

    private void removeOldBinary(File homeDir) {
        File binDir = getBinaryRoot(homeDir);
        if (binDir != null) {
            File[] files = binDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && !StringUtils.equals(file.getName(), IDE_INFO_MAP.get("version"))) {
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
        }
    }

    public boolean initBinary(File binDir) {
        File zipTmpDir = buildTempDir();
        try {
            AgentInfoResp agentInfoResp = fetchLatestAgentInfo(getVersion());
            String localBinaryMd5 = StringUtils.EMPTY;

            if (agentInfoResp != null && agentInfoResp.isAutoUpgradeEnabled()) {
                String versionBinaryPath = getBinaryVersionPath(binDir, agentInfoResp.getVersion());
                File versionBinaryFile = new File(versionBinaryPath);
                if (versionBinaryFile.exists()) {
                    localBinaryMd5 = getFileMd5(versionBinaryFile);
                }
                String latestVersion = agentInfoResp.getVersion();
                String latestMd5 = agentInfoResp.getMd5();
                LOG.info("Retrieved agent info for " + COMPATIBLE_ARCH + ", version: " + latestVersion + ", md5: " + latestMd5);

                if (StringUtils.equalsIgnoreCase(localBinaryMd5, agentInfoResp.getArchMd5().get(COMPATIBLE_ARCH))) {
                    LOG.info("Local environment is already upgraded.");
                    IDE_INFO_MAP.put("version", agentInfoResp.getVersion());
                    return Boolean.TRUE;
                }

                // 查看本地是否已下载
                File upgradeDir = new File(getHomeDir(), "upgrade");
                Pair<File, String> localUpgrade = findLatestLocalUpgradeZip(upgradeDir);
                if (localUpgrade != null) {
                    String localMd5 = localUpgrade.second;
                    File latestAgentZip;
                    if (!StringUtils.equalsIgnoreCase(localMd5, bundledMD5Cache.get("zip"))) {
                        if (StringUtils.equalsIgnoreCase(localMd5, latestMd5)) {
                            latestAgentZip = localUpgrade.first;
                            LOG.info("New agent is already downloaded.");
                        } else {
                            LOG.info("Downloading new version: " + latestVersion + ", with zip md5:[" + agentInfoResp.getMd5() + "].");
                            localUpgrade.first.delete();
                            latestAgentZip = downloadAgent(agentInfoResp.getUrl(), latestVersion, upgradeDir);
                        }
                        if (latestAgentZip != null) {
                            removeOldBinary(getHomeDir());
                            unzipAndCopy(latestAgentZip, zipTmpDir, binDir);
                            IDE_INFO_MAP.put("version", agentInfoResp.getVersion());
                            return Boolean.TRUE;
                        }
                    }
                }
            }
            LOG.info("Using bundled version.");
            String bundledBinaryPath = getBinaryVersionPath(binDir, BUNDLED_VERSION);
            File bundledBinaryFile = new File(bundledBinaryPath);
            if (bundledBinaryFile.exists()) {
                localBinaryMd5 = getFileMd5(bundledBinaryFile);
            }

            if (StringUtils.equalsIgnoreCase(localBinaryMd5, bundledMD5Cache.get(COMPATIBLE_ARCH))) {
                LOG.info("Local agent is same as bundled version.");
                IDE_INFO_MAP.put("version", BUNDLED_VERSION);
                return Boolean.TRUE;
            }

            removeOldBinary(getHomeDir());
            File archDir = unZipBinary(zipTmpDir.getAbsolutePath());
            if (archDir == null || !archDir.exists()) {
                LOG.warn("解压内嵌二进制文件失败: " + COMPATIBLE_ARCH);
                return false;
            }
            File versionDir = archDir.getParentFile();
            IDE_INFO_MAP.put("version", versionDir.getName());
            copyDirs(versionDir, binDir);
        } catch (Exception e) {
            LOG.warn("Error occurred while processing files", e);
            return false;
        } finally {
            try {
                FileUtils.deleteDirectory(zipTmpDir);
            } catch (IOException ex) {
                LOG.warn("Error occurred while deleting temporary directory", ex);
            }
        }
        return true;
    }

    /**
     * 查找 upgrade 目录下最新的 zip 及其md5值
     */
    private Pair<File, String> findLatestLocalUpgradeZip(File upgradeDir) {
        if (!upgradeDir.exists() || !upgradeDir.isDirectory()) {
            return null;
        }
        File latestZip = null;
        String md5 = null;
        File[] versionDirs = upgradeDir.listFiles(File::isDirectory);
        if (versionDirs != null) {
            for (File versionDir : versionDirs) {
                File zipFile = new File(versionDir, "DevPilot.zip");
                if (zipFile.exists() && zipFile.isFile()) {
                    latestZip = zipFile;
                    md5 = getFileMd5(latestZip);
                }
            }
        }
        return latestZip != null ? new Pair<>(latestZip, md5) : null;
    }

    private String getFileMd5(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            return DigestUtils.md5Hex(fileInputStream);
        } catch (Exception e) {
            return "";
        }
    }

    private void unzipAndCopy(File latestAgentZip, File zipTmpDir, File binDir) throws IOException {
        try (FileInputStream agentStream = new FileInputStream(latestAgentZip)) {
            Path targetDir = Paths.get(zipTmpDir.getAbsolutePath());
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            File archDir = unzipFile(agentStream, targetDir);
            File versionDir = archDir.getParentFile();
            IDE_INFO_MAP.put("version", versionDir.getName());
            copyDirs(versionDir, binDir);
        }
    }

    private void copyDirs(File source, File target) {
        try {
            File targetDir = new File(target, source.getName());
            if (targetDir.exists()) {
                FileUtils.copyDirectoryToDirectory(source, target);
            } else {
                FileUtils.moveDirectoryToDirectory(source, target, true);
            }
        } catch (Exception e) {
            LOG.warn("Error occurred while copyDirs.", e);
        }
    }

    public Pair<Integer, Long> retrieveAlivePort() {
        File homeDir = getHomeDir();
        if (homeDir != null) {
            File infoFile = new File(homeDir, getIdeInfoPath());
            if (infoFile.exists()) {
                return checkInfoFile(infoFile);
            }
        }
        return null;
    }

    public void findProcessAndKill() {
        File homeDir = getHomeDir();
        if (null != homeDir) {
            findProcessAndKill(homeDir);
        }
    }

    public void findProcessAndKill(File homeDir) {
        Pair<Integer, Long> infoPair = readProcessInfoFile(homeDir);
        if (infoPair != null && infoPair.second != null) {
            LOG.info(String.format("Try to kill process: %s.", infoPair.second));
            killProcessAndDeleteInfoFile(infoPair.second, true);
        } else {
            LOG.info("Pid not exist when trying to kill process, skip process killing");
            List<Long> pidList = ProcessUtils.findDevPilotAgentPidList(NumberUtils.LONG_ZERO);
            if (!pidList.isEmpty()) {
                LOG.info(String.format("Find %s process(es) when trying to kill process.", pidList.size()));
                for (Long pid : pidList) {
                    killProcessAndDeleteInfoFile(pid, true);
                }
            } else {
                LOG.info("No process found, skip killing process.");
            }
        }
    }

    public void killOldProcess(Long pid) {
        if (pid == null) {
            return;
        }
        LOG.info(String.format("Start to kill old process: %s.", pid));
        killProcessAndDeleteInfoFile(pid, false);
    }

    private Pair<Integer, Long> readProcessInfoFile(File homeDir) {
        int maxRetryTimes = NumberUtils.INTEGER_ONE;
        File infoFile = new File(homeDir, getIdeInfoPath());
        for (int i = 0; i < maxRetryTimes; i++) {
            Pair<Integer, Long> infoPair = checkInfoFile(infoFile);
            if (infoPair != null) {
                return infoPair;
            }
            List<Long> pidList = ProcessUtils.findDevPilotAgentPidList(NumberUtils.LONG_ZERO);
            for (Long pid : pidList) {
                killOldProcess(pid);
            }
        }
        return null;
    }

    private void killProcessAndDeleteInfoFile(Long pid, boolean needDel) {
        if (pid > 0L && ProcessUtils.isProcessAlive(pid)) {
            LOG.info(String.format("Try to kill %d.", pid));
            ProcessUtils.killProcess(pid);
        }
        if (needDel) {
            this.deleteInfoFile();
        }
    }

    private void deleteInfoFile() {
        File homeDir = this.getHomeDir();
        if (homeDir != null) {
            File infoFile = new File(homeDir, getIdeInfoPath());
            if (infoFile.exists()) {
                try {
                    infoFile.delete();
                    LOG.info("Delete info file success.");
                } catch (Exception e) {
                    LOG.warn("Delete info file encountered exception", e);
                }
            }
        }
    }

    private static Pair<Integer, Long> checkInfoFile(@NotNull File infoFile) {
        if (infoFile.exists()) {
            try {
                String rawText = FileUtils.readFileToString(infoFile, StandardCharsets.UTF_8);
                if (rawText != null && !rawText.isEmpty()) {
                    String[] lines = rawText.split("\r\n|\n");
                    if (lines.length < 2) {
                        LOG.info("Read info file get invalided port and pid.");
                        return null;
                    }

                    int port = Integer.parseInt(lines[0]);
                    Long pid = Long.valueOf(lines[1]);
                    LOG.debug("Read info file get port:" + port + ", pid:" + pid);
                    return new Pair<>(port, pid);
                }

                LOG.debug("info file is empty, check failed");
                return null;
            } catch (Throwable e) {
                LOG.warn("Error occurred while checking info file.", e);
            }
        }
        return null;
    }

    private String getUserHome() {
        String userHome = null;
        String osName = System.getProperty("os.name");
        if (StringUtils.isNotBlank(osName)) {
            osName = osName.toLowerCase();
            if (ProcessUtils.isWindowsPlatform()) {
                userHome = System.getenv("USERPROFILE");
            } else if (osName.contains("mac")) {
                userHome = System.getenv("HOME");
            } else if (osName.contains("nix") || osName.contains("nux")) {
                userHome = System.getenv("HOME");
            }
        }
        return StringUtils.isBlank(userHome) ? System.getProperty("user.home") : userHome;
    }

    private Path getHomeDirectory() {
        var localStoragePath = PersonalAdvancedSettingsState.getInstance().getLocalStorage();
        if (StringUtils.isNotBlank(localStoragePath)) {
            File file = new File(localStoragePath);
            boolean validPath = true;
            if (!file.exists()) {
                validPath = file.mkdirs();
            }

            if (validPath) {
                return Paths.get(localStoragePath);
            }
        }
        return getDefaultHomePath();
    }

    private File getBinaryRoot(@NotNull File workDirectory) {
        File dir = new File(workDirectory, "bin");
        return !dir.exists() && !dir.mkdirs() ? null : dir;
    }

    private String getDefaultBinaryPath(File root) {
        Optional<File> versionDir = Arrays.stream(Optional.ofNullable(root.listFiles()).orElse(new File[]{}))
                .filter(File::isDirectory)
                .filter(dir -> StringUtils.equals(dir.getName(), IDE_INFO_MAP.get("version")))
                .findFirst();

        if (versionDir.isPresent()) {
            String dirName = versionDir.get().getName();
            checkBinaryPermissions(root, dirName);
            return getBinaryVersionPath(root, dirName);
        } else {
            return null;
        }
    }

    private void checkBinaryPermissions(File root, String version) {
        File versionDir = Paths.get(root.getAbsolutePath(), version, COMPATIBLE_ARCH).toFile();
        if (versionDir.exists()) {
            versionDir.setExecutable(true);
        }

        File[] files = versionDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.setExecutable(true);
            }
        }
    }

    private String getBinaryVersionPath(File root, String version) {
        return Paths.get(root.getAbsolutePath(), version, COMPATIBLE_ARCH, getExecutableName()).toString();
    }

    private static String getExecutableName() {
        return SystemInfo.isWindows ? EXECUTABLE_NAME + ".exe" : EXECUTABLE_NAME;
    }

    private File unZipBinary(String destDirPath) throws Exception {
        Path targetDir = Paths.get(destDirPath);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        try (InputStream stream = BinaryManager.class.getResourceAsStream("/binaries/DevPilot.zip")) {
            if (stream == null) {
                return null;
            }
            return unzipFile(stream, targetDir);
        }
    }

    private File unzipFile(InputStream stream, Path targetDir) {
        File finalDir = null;

        try (ZipInputStream zis = new ZipInputStream(stream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String[] parts = entry.getName().split("[/\\\\]");
                if (parts.length <= 2 || !needUnzip(parts[2])) {
                    continue;
                }

                Path targetPath = targetDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    handleDirectory(targetPath, parts);
                    if (StringUtils.equalsIgnoreCase(parts[parts.length - 1], COMPATIBLE_ARCH)) {
                        finalDir = targetPath.toFile();
                    }
                } else {
                    handleFile(targetPath, zis);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while unzipping file.", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return finalDir;
    }

    private static void handleDirectory(Path targetPath, String[] parts) throws IOException {
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath);
        }
    }

    private static void handleFile(Path targetPath, ZipInputStream zis) throws IOException {
        Files.createDirectories(targetPath.getParent());
        if (Files.exists(targetPath)) {
            Files.delete(targetPath);
        }
        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }

        targetPath.toFile().setExecutable(true);
    }

    private static boolean needUnzip(String entryName) {
        return StringUtils.equalsIgnoreCase(entryName, COMPATIBLE_ARCH) || StringUtils.equals(entryName, "extension");
    }

    private static String getSystemArch() {
        String arch = CpuArch.is32Bit() ? "i686" : X86_64;
        if (AARCH64.equals(System.getProperty("os.arch"))) {
            arch = AARCH64;
        }
        return arch;
    }

    public String getIdeType() {
        return IDE_INFO_MAP.get("type");
    }

    private static String getPlatformName() {
        String platform;
        if (SystemInfo.isWindows) {
            platform = WINDOWS_PLATFORM;
        } else if (SystemInfo.isMac) {
            platform = MAC_PLATFORM;
        } else {
            if (!SystemInfo.isLinux) {
                LOG.warn("DevPilot only supports platform Windows, macOS, Linux");
                throw new RuntimeException("DevPilot only supports platform Windows, macOS, Linux");
            }
            platform = LINUX_PLATFORM;
        }
        return platform;
    }

    private File buildTempDir() {
        return new File(System.getProperty("java.io.tmpdir"), String.format("devpilot_%d", System.currentTimeMillis()));
    }

    /**
     * 定时任务里面调用判断是否需要自动升级
     *
     * @return
     */
    public boolean checkIfAutoUpgradeNeeded() {
        try {
            AgentInfoResp agentInfoResp = fetchLatestAgentInfo(getVersion());
            if (agentInfoResp == null) {
                LOG.warn("Failed to fetch latest agent info.");
                return false;
            }
            if (!agentInfoResp.isAutoUpgradeEnabled()) {
                LOG.info("Auto upgrade is disabled.");
                return false;
            }
            String localBinaryPath = getBinaryPath(getHomeDir());
            String localBinaryMd5 = Optional.ofNullable(localBinaryPath).map(path -> getFileMd5(new File(path))).orElse(StringUtils.EMPTY);

            String dstBinaryMd5 = agentInfoResp.getArchMd5().get(COMPATIBLE_ARCH);
            if (!StringUtils.equalsIgnoreCase(localBinaryMd5, dstBinaryMd5)) {
                LOG.info("New agent available: " + dstBinaryMd5 + ", current agent md5 is: " + localBinaryMd5);
                return Boolean.TRUE;
            }
        } catch (Exception e) {
            LOG.warn("Error checking for agent upgrade", e);
        }
        return false;
    }

    private AgentInfoResp fetchLatestAgentInfo(String curVersion) {
        String host = TELEMETRY_HOST;
        if (StringUtils.isEmpty(host)) {
            return null;
        }
        String platformName = getPlatformName();
        String arch = getSystemArch();
        String url = String.format(host + "/fetch-latest-agent-info?version=%s&arch=%s&platform=%s",
                curVersion, arch, platformName);
        Request request = new Request.Builder()
                .header("User-Agent", UserAgentUtils.buildUserAgent())
                .header("Auth-Type", LoginUtils.getLoginType())
                .url(url).get().build();
        Call call = OkhttpUtils.getClient().newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            ResponseBody body = response.body();
            if (body != null) {
                String bodyString = body.string();
                if (StringUtils.isNotEmpty(bodyString)) {
                    return JsonUtils.fromJson(bodyString, AgentInfoResp.class);
                }
            }
            return null;
        } catch (Exception e) {
            LOG.warn("fetchLatestAgentInfo error: " + e);
            return null;
        }
    }

    private File downloadAgent(String downloadUrl, String latestVersion, File upgradeDir) {
        try {
            URL url = new URL(downloadUrl);
            File versionDir = new File(upgradeDir, latestVersion);
            if (!versionDir.exists()) {
                Files.createDirectories(versionDir.toPath());
            }
            // 删除旧版本的目录
            File[] oldDirs = upgradeDir.listFiles(File::isDirectory);
            if (oldDirs != null) {
                for (File oldDir : oldDirs) {
                    if (!StringUtils.equals(oldDir.getName(), latestVersion)) {
                        try {
                            FileUtils.deleteDirectory(oldDir);
                        } catch (IOException ex) {
                            LOG.warn("Failed to delete old directory", ex);
                        }
                    }
                }
            }

            File zipFile = new File(versionDir, "DevPilot.zip");
            try (InputStream in = url.openStream();
                 FileOutputStream out = new FileOutputStream(zipFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return zipFile;
        } catch (Exception e) {
            LOG.warn("Failed to download agent binary.", e);
            return null;
        }
    }

    public static class AgentCheckResult {

        private final boolean isRunning;

        private final Long pid;

        private final Integer port;

        public AgentCheckResult(boolean isRunning, Long pid, Integer port) {
            this.isRunning = isRunning;
            this.pid = pid;
            this.port = port;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public Long getPid() {
            return pid;
        }

        public Integer getPort() {
            return port;
        }

    }

}
