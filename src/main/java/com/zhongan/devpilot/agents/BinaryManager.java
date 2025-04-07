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
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    static {
        COMPATIBLE_ARCH = String.format("%s_%s", getSystemArch(), getPlatformName());
        IDE_INFO_MAP.put("type", DevPilotVersion.getVersionName().replace(" ", "_"));
    }

    public String getIdeInfoPath() {
        return "." + IDE_INFO_MAP.get("type") + "info";
    }

    public String getVersion() {
        return IDE_INFO_MAP.get("version");
    }

    public String getType() {
        return IDE_INFO_MAP.get("type");
    }

    public String getCompatibleArch() {
        return COMPATIBLE_ARCH;
    }

    @Contract(pure = true)
    private BinaryManager() {
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
            File finalHomeDir = new File(homeDir, IDE_INFO_MAP.get("type"));
            if (!finalHomeDir.exists() && !finalHomeDir.mkdirs()) {
                LOG.warn("Failed to create final home directory." + finalHomeDir.getName());
                return null;
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
            Response response = call.execute();
            isRunning = response.isSuccessful();
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
                    if (file.isDirectory() && !file.getName().equals(IDE_INFO_MAP.get("version"))) {
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
            // 检查 autoUpgradeEnabled，如果为 false，直接使用内嵌版本
            if (agentInfoResp != null && !agentInfoResp.isAutoUpgradeEnabled()) {
                LOG.info("Auto upgrade is disabled, using bundled version");
                File archDir = unZipBinary(zipTmpDir.getAbsolutePath());
                if (archDir == null || !archDir.exists()) {
                    LOG.warn("Failed to unzip bundled binary for " + COMPATIBLE_ARCH);
                    return false;
                }
                File versionDir = archDir.getParentFile();
                IDE_INFO_MAP.put("version", versionDir.getName());
                copyDirs(versionDir, binDir);
                removeOldBinary(getHomeDir());
                return true;
            }
            File upgradeDir = new File(getHomeDir(), "upgrade");
            Pair<File, String> localUpgrade = findLatestLocalUpgradeZip(upgradeDir);
            String bundledVersion = extractBundledAgentVersion();
            if (agentInfoResp == null) {
                LOG.warn("Failed to fetch latest agent info, checking local upgrade directory...");
                // 检查 upgrade 目录的 zip 是否比内嵌 zip 版本高
                if (localUpgrade != null && compareAgentVersions(localUpgrade.second, bundledVersion) > 0) {
                    LOG.info("Using upgrade directory zip: " + localUpgrade.first.getName());
                    unzipAndCopy(localUpgrade.first, zipTmpDir, binDir);
                    removeOldBinary(getHomeDir());
                } else {
                    // upgrade 目录不存在 zip 或版本较低，使用内嵌 zip
                    LOG.info("Using bundled zip version: " + bundledVersion);
                    File archDir = unZipBinary(zipTmpDir.getAbsolutePath());
                    if (archDir == null || !archDir.exists()) {
                        LOG.warn("Fail to unzip binary for " + COMPATIBLE_ARCH);
                        return false;
                    }
                    File versionDir = archDir.getParentFile();
                    IDE_INFO_MAP.put("version", versionDir.getName());
                    copyDirs(versionDir, binDir);
                    removeOldBinary(getHomeDir());
                }
            } else {
                String latestVersion = agentInfoResp.getVersion();
                String latestMd5 = agentInfoResp.getMd5();
                File latestAgentZip;
                // 1. 如果 upgrade 目录 zip 版本更高，直接使用
                if (localUpgrade != null && compareAgentVersions(localUpgrade.second, latestVersion) > 0) {
                    LOG.info("Using higher version from upgrade directory: " + localUpgrade.first.getName());
                    unzipAndCopy(localUpgrade.first, zipTmpDir, binDir);
                    removeOldBinary(getHomeDir());
                } else if (localUpgrade != null && compareAgentVersions(localUpgrade.second, latestVersion) == 0) { // 2. 如果 upgrade 目录 zip 版本相同，检查 MD5
                    String localMd5 = getFileMd5(localUpgrade.first);
                    if (localMd5.equals(latestMd5)) {
                        LOG.info("Local upgrade zip matches remote version and MD5, using cached zip.");
                        unzipAndCopy(localUpgrade.first, zipTmpDir, binDir);
                        removeOldBinary(getHomeDir());
                    } else {
                        LOG.warn("Local zip MD5 mismatch, deleting old zip and downloading new version.");
                        localUpgrade.first.delete();
                        latestAgentZip = downloadAgent(agentInfoResp.getUrl(), latestVersion, upgradeDir);
                        if (latestAgentZip != null) {
                            unzipAndCopy(latestAgentZip, zipTmpDir, binDir);
                            removeOldBinary(getHomeDir());
                        }
                    }
                } else { // 3. 如果 upgrade 目录 zip 版本较低，删除旧文件并下载最新 zip
                    LOG.info("Downloading new version: " + latestVersion);
                    if (localUpgrade != null) {
                        localUpgrade.first.delete();
                    }
                    latestAgentZip = downloadAgent(agentInfoResp.getUrl(), latestVersion, upgradeDir);
                    if (latestAgentZip != null) {
                        unzipAndCopy(latestAgentZip, zipTmpDir, binDir);
                        removeOldBinary(getHomeDir());
                    }
                }
            }
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
     * 查找 upgrade 目录下最新的 zip 及其版本号
     */
    private Pair<File, String> findLatestLocalUpgradeZip(File upgradeDir) {
        if (!upgradeDir.exists() || !upgradeDir.isDirectory()) {
            return null;
        }
        File latestZip = null;
        String latestVersion = null;
        File[] versionDirs = upgradeDir.listFiles(File::isDirectory);
        if (versionDirs != null) {
            for (File versionDir : versionDirs) {
                String version = versionDir.getName();
                File zipFile = new File(versionDir, "DevPilot.zip");
                if (zipFile.exists() && zipFile.isFile()) {
                    if (latestVersion == null || compareAgentVersions(version, latestVersion) > 0) {
                        latestVersion = version;
                        latestZip = zipFile;
                    }
                }
            }
        }
        return latestZip != null ? new Pair<>(latestZip, latestVersion) : null;
    }

    private String getFileMd5(File file) {
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);
            String localMd5 = Base64.getEncoder().encodeToString(DigestUtils.md5(fileInputStream));
            return localMd5;
        } catch (Exception e) {
            return "";
        } finally {
            IOUtils.closeQuietly(fileInputStream);
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
            List<Long> pidList = ProcessUtils.findDevPilotAgentPidList();
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
                .filter(dir -> dir.getName().equals(IDE_INFO_MAP.get("version")))
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
                    if (parts[parts.length - 1].equalsIgnoreCase(COMPATIBLE_ARCH)) {
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
        Files.createFile(targetPath);
        FileUtils.copyToFile(zis, targetPath.toFile());
        targetPath.toFile().setExecutable(true);
    }

    private static boolean needUnzip(String entryName) {
        return entryName.equalsIgnoreCase(COMPATIBLE_ARCH) || entryName.equals("extension");
    }

    private String extractBundledAgentVersion() {
        File zipTmpDir = buildTempDir();
        try {
            File archDir = unZipBinary(zipTmpDir.getAbsolutePath());
            if (archDir == null || !archDir.exists()) {
                return "";
            }
            File versionDir = archDir.getParentFile();
            return versionDir.getName();
        } catch (Exception e) {
            return "";
        } finally {
            try {
                FileUtils.deleteDirectory(zipTmpDir);
            } catch (IOException ignore) {
            }
        }
    }

    private static String getSystemArch() {
        String arch = CpuArch.is32Bit() ? "i686" : "x86_64";
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            arch = "aarch64";
        }
        return arch;
    }

    public String getIdeType() {
        return IDE_INFO_MAP.get("type");
    }

    private static String getPlatformName() {
        String platform;
        if (SystemInfo.isWindows) {
            platform = "windows";
        } else if (SystemInfo.isMac) {
            platform = "darwin";
        } else {
            if (!SystemInfo.isLinux) {
                LOG.warn("DevPilot only supports platform Windows, macOS, Linux");
                throw new RuntimeException("DevPilot only supports platform Windows, macOS, Linux");
            }
            platform = "linux";
        }
        return platform;
    }

    private File buildTempDir() {
        return new File(System.getProperty("java.io.tmpdir"), String.format("devpilot_%d", System.currentTimeMillis()));
    }

    public void upgradeAgent() {
        BinaryManager.INSTANCE.findProcessAndKill();
        AgentsRunner.INSTANCE.run(true);
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
            String latestVersion = agentInfoResp.getVersion();
            String currentVersion = IDE_INFO_MAP.get("version");
            if (currentVersion != null && compareAgentVersions(latestVersion, currentVersion) > 0) {
                LOG.info("New agent version available: " + latestVersion + ", current version: " + currentVersion);
                return true;
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
        try {
            Response response = call.execute();
            if (!response.isSuccessful()) {
                return null;
            }
            ResponseBody body = response.body();
            if (body != null) {
                return JsonUtils.fromJson(body.string(), AgentInfoResp.class);
            } else {
                return null;
            }
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
                    if (!oldDir.getName().equals(latestVersion)) {
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

    public int compareAgentVersions(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (part1 < part2) {
                return -1;
            } else if (part1 > part2) {
                return 1;
            }
        }
        return 0;
    }

}
