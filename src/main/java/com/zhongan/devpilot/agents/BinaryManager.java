package com.zhongan.devpilot.agents;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
import com.zhongan.devpilot.DevPilotVersion;
import com.zhongan.devpilot.settings.state.PersonalAdvancedSettingsState;
import com.zhongan.devpilot.util.OkhttpUtils;
import com.zhongan.devpilot.util.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.Call;
import okhttp3.Request;

import okhttp3.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class BinaryManager {
    private static final Logger LOG = Logger.getInstance(BinaryManager.class);

    public static final BinaryManager INSTANCE = new BinaryManager();

    private static final String COMPATIBLE_ARCH;

    private static final Map<String, String> IDE_INFO_MAP = new HashMap<>();

    public static final String EXECUTABLE_NAME = "devpilot-agents";

    private volatile Integer currentPort;

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

    public String getCompatibleArch() {
        return COMPATIBLE_ARCH;
    }

    @Contract(pure = true)
    private BinaryManager() {
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


    public synchronized boolean postProcessBeforeRunning(File homeDir) throws Exception {
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
        if (infoPair != null && infoPair.second != null) {
            if (agentAvailable(infoPair.first, infoPair.second)) {
                LOG.info(String.format("Finding agent is running on port: [%s], pid: [%s]", infoPair.first, infoPair.second));
                return new AgentCheckResult(true, infoPair.second, infoPair.first);
            }
        }
        return new AgentCheckResult(false, null, null);
    }

    private boolean agentAvailable(Integer port, Long pid) {
        if (port == null || pid == null) {
            return false;
        }
        boolean isRunning = ProcessUtils.isProcessAlive(pid);
        if (!isRunning) {
            return false;
        }
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

    public synchronized void clearDataBefore(String oldHomePath) {
        try {
            LOG.info("Clearing old homePath data:" + oldHomePath);
            File oldHomeDir = new File(oldHomePath);
            if (!oldHomeDir.exists()) {
                return;
            }
            // kill old process
            File oldInfoFile = new File(oldHomeDir, IDE_INFO_MAP.get("type") + File.separator + getIdeInfoPath());
            if (oldInfoFile.exists()) {
                Pair<Integer, Long> oldPortAndPid = checkInfoFile(oldInfoFile);
                if (oldPortAndPid != null) {
                    killOldProcess(oldPortAndPid.second);
                }
            }
            FileUtils.deleteDirectory(oldHomeDir);
        } catch (IOException e) {
            LOG.warn("Failed to clear old home path data", e);
        }
    }


    public boolean initBinary(File homeDir) throws Exception {
        File zipTmpDir = new File(System.getProperty("java.io.tmpdir"), String.format("devpilot_%d", System.currentTimeMillis()));
        try {
            File archDir = unZipBinary(zipTmpDir.getAbsolutePath());
            if (archDir == null || !archDir.exists()) {
                LOG.warn("Fail to unzip binary for " + COMPATIBLE_ARCH);
                return false;
            }

            File versionDir = archDir.getParentFile();
            IDE_INFO_MAP.put("version", versionDir.getName());

            File targetDir = new File(homeDir, versionDir.getName());
            if (targetDir.exists()) {
                FileUtils.copyDirectoryToDirectory(versionDir, homeDir);
            } else {
                FileUtils.moveDirectoryToDirectory(versionDir, homeDir, true);
            }

            File[] files = zipTmpDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isDirectory()) {
                        FileUtils.copyFile(file, new File(homeDir, file.getName()));
                    }
                }
            }
        } catch (FileExistsException e) {
            LOG.warn("Error occurred while coping file.", e);
        } finally {
            try {
                FileUtils.deleteDirectory(zipTmpDir);
            } catch (IOException ex) {
                LOG.warn("Error occurred while deleting file.", ex);
            }
        }
        return true;
    }

    public Pair<Integer, Long> retrieveAlivePort() {
        if (currentPort != null) {
            return new Pair<>(currentPort, 0L);
        }
        File homeDir = getHomeDir();
        if (homeDir != null) {
            File infoFile = new File(homeDir, getIdeInfoPath());
            if (infoFile.exists()) {
                return checkInfoFile(infoFile);
            }
        }
        return null;
    }

    public void setCurrentPort(Integer port) {
        this.currentPort = port;
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

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                LOG.info("Thread sleep is interrupted when waiting for info file");
            }

            if (shouldExtendRetries(i, maxRetryTimes)) {
                maxRetryTimes = extendRetries(maxRetryTimes);
            }
        }

        return findProcessAndPortByName();
    }

    private boolean shouldExtendRetries(int currentAttempt, int maxAttempts) {
        return SystemInfo.isWindows && currentAttempt == maxAttempts - 1 && !CollectionUtils.isEmpty(ProcessUtils.findDevPilotAgentPidList());
    }

    private int extendRetries(int maxRetryTimes) {
        List<Long> pidList = ProcessUtils.findDevPilotAgentPidList();
        String pidListStr = StringUtils.join(pidList, ",");
        LOG.info(String.format("Found pid list: %s, delay max retry times", StringUtils.defaultString(pidListStr, "null")));
        return maxRetryTimes * 3;
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

    private Pair<Integer, Long> findProcessAndPortByName() {
        List<Long> pidList = ProcessUtils.findDevPilotAgentPidList();
        String pidListStr = StringUtils.join(pidList, ",");
        LOG.debug(String.format("Found pid list: %s", pidListStr == null ? "null" : pidListStr));
        if (CollectionUtils.isNotEmpty(pidList)) {
            return new Pair<>(3000, pidList.get(0));
        } else {
            return ProcessUtils.isWindowsPlatform() ? new Pair<>(3000, 0L) : null;
        }
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
