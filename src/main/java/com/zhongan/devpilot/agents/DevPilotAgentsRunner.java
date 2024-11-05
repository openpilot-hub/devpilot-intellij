package com.zhongan.devpilot.agents;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.settings.state.PersonalAdvancedSettingsState;
import com.zhongan.devpilot.util.ProcessUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class DevPilotAgentsRunner {

    public synchronized boolean run() throws Exception {

        File homeDir = getHomeDir();
        if (homeDir == null) {
            return false;
        }
        DevPilotNotification.info(homeDir.getAbsolutePath());

        // TODO:: 需要判断是否已启动了，且要匹配版本，以便做agent的版本更新
        //        if (version !=) {
        //            findProcessAndKill();
        //        }
        findProcessAndKill();
        BinaryManager.INSTANCE.initBinary(getBinaryRoot(homeDir));

        String binaryPath = getBinaryPath(homeDir);
        boolean aliveFlag = checkProcess();
        DevPilotNotification.info("aliveFlag: " + aliveFlag);
        if (aliveFlag) {
            return true;
        }

        int port = getAvailablePort();
        List<String> commands = createCommand(binaryPath, port);
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(getHomeDirectory().toFile());
        Process process = builder.start();

        aliveFlag = process.isAlive();
        if (aliveFlag) {
            writeInfoFile(ProcessUtils.findDevPilotAgentPidList(), port);
        }
        return aliveFlag;
    }

    private int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("No available port", e);
        }
    }

    public void writeInfoFile(List<Long> pids, int port) {
        File homeDir = getHomeDir();
        if (homeDir != null) {
            File infoFile = new File(homeDir, ".info");
            try (FileWriter writer = new FileWriter(infoFile)) {
                writer.write(port + System.lineSeparator());
                for (Long pid : pids) {
                    writer.write(pid + System.lineSeparator());
                }
                DevPilotNotification.info("Write .info file success.");
            } catch (IOException e) {
                DevPilotNotification.warn("Failed to write .info file: " + e.getMessage());
            }
        }
    }

    public String getBinaryPath(@NotNull File workDirectory) throws IOException {
        File root = getBinaryRoot(workDirectory);
        if (root == null) {
            return null;
        } else {
            return getDefaultBinaryPath(root);
        }
    }

    private File getBinaryRoot(@NotNull File workDirectory) {
        File dir = new File(workDirectory, "bin");
        return !dir.exists() && !dir.mkdirs() ? null : dir;
    }

    private String getDefaultBinaryPath(File root) {
        Optional<File> latestDir = Arrays.stream(Optional.ofNullable(root.listFiles()).orElse(new File[]{}))
                .filter(File::isDirectory)
                .max(Comparator.comparingLong(File::lastModified));

        if (latestDir.isPresent()) {
            String dirName = latestDir.get().getName();
            checkBinaryPermissions(root, dirName);
            return getBinaryVersionPath(root, dirName);
        } else {
            return null;
        }
    }

    private void checkBinaryPermissions(File root, String version) {
        File versionDir = Paths.get(root.getAbsolutePath(), version, BinaryManager.VALID_ARCH).toFile();
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
        return Paths.get(root.getAbsolutePath(), version, BinaryManager.VALID_ARCH, getExecutableName()).toString();
    }

    private static String getExecutableName() {
        return SystemInfo.isWindows ? "devpilot-agents.exe" : "devpilot-agents";
    }

    private static File getHomeDir() {
        File homeDir = getHomeDirectory().toFile();
        if (!homeDir.exists() && !homeDir.mkdirs()) {
            DevPilotNotification.warn("Failed to create home directory.");
            return null;
        } else {
            return homeDir;
        }
    }

    public static Path getHomeDirectory() {
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
        return Paths.get(getUserHome(), ".DevPilot");
    }

    private static String getUserHome() {
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

    private List<String> createCommand(@NotNull String binaryPath, int port) {

        List<String> commands = new ArrayList<>();
        if (ProcessUtils.isWindowsPlatform()) {
            String command = "cmd";
            File cmdFile = new File(System.getenv("WINDIR") + "\\system32\\cmd.exe");
            if (!cmdFile.exists()) {
                cmdFile = new File("c:\\Windows\\system32\\cmd.exe");
            }

            if (cmdFile.exists()) {
                command = "\"" + cmdFile.getAbsolutePath() + "\"";
            }

            commands.add(command);
            commands.add("/c");
        }

        if (ProcessUtils.isWindowsPlatform()) {
            binaryPath = binaryPath.replace("(", "^(").replace(")", "^)").replace("&", "^&").replace(">", "^>").replace("<", "^<").replace("|", "^|");
            binaryPath = "\"" + binaryPath + "\"";
        }

        commands.add(binaryPath);
        commands.add("--port");
        commands.add(String.valueOf(port));
        return commands;
    }

    public boolean checkProcess() {
        Pair<Integer, Long> infoPair = retrieveAlivePort();
        if (infoPair != null) {
            Integer port = infoPair.first;
            Long pid = infoPair.second;
            return pid != null && port != null && ProcessUtils.isProcessAlive(pid);
        }
        return false;
    }

    public static Pair<Integer, Long> retrieveAlivePort() {
        File homeDir = getHomeDir();
        if (homeDir != null) {
            File infoFile = new File(homeDir, ".info");
            if (infoFile.exists()) {
                return checkInfoFile(infoFile);
            }
        }
        return null;
    }


    private static Pair<Integer, Long> checkInfoFile(@NotNull File infoFile) {
        if (!infoFile.exists()) {
            DevPilotNotification.info(".info file not exist, wait 100ms and retry");
        } else {
            try {
                String rawText = FileUtils.readFileToString(infoFile, StandardCharsets.UTF_8);
                if (rawText != null && !rawText.isEmpty()) {
                    String[] lines = rawText.split("\r\n|\n");

                    int port = Integer.parseInt(lines[0]);
                    Long pid = Long.valueOf(lines[1]);
                    DevPilotNotification.info("Read.info file get port:" + port + ", pid:" + pid);
                    return new Pair<>(port, pid);
                }

                DevPilotNotification.warn(".info file is empty, check failed");
                return null;
            }  catch (Throwable e) {
                DevPilotNotification.warn("Check info file encountered Throwable" + e.getMessage());
            }
        }

        return null;
    }

    public void findProcessAndKill() {
        Pair<Integer, Long> infoPair = readProcessInfoFile(1);
        if (infoPair != null && infoPair.second != null) {
            killProcessAndDeleteInfoFile(infoPair.second);
        } else {
            DevPilotNotification.info("Pid not exist when trying to kill process, skip process killing");
        }
    }

    public Pair<Integer, Long> readProcessInfoFile(int maxRetryTimes) {
        File homeDir = getHomeDir();
        if (homeDir == null) {
            return null;
        } else {
            File infoFile = new File(homeDir, ".info");
            Integer port = null;
            Long pid = null;
            boolean delay = false;
            int i = 0;

            while(i < maxRetryTimes) {
                Pair<Integer, Long> infoPair = this.checkInfoFile(infoFile);
                if (infoPair != null) {
                    port = infoPair.first;
                    pid = infoPair.second;
                    break;
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    DevPilotNotification.warn("Thread sleep is interrupted when waiting for .info file");
                }

                ++i;
                DevPilotNotification.info(String.format("Retry for fetching .info for %d times, %d times left", i, maxRetryTimes - i));
                if (!delay && SystemInfo.isWindows && i == maxRetryTimes) {
                    delay = true;
                    List<Long> pidList = ProcessUtils.findDevPilotAgentPidList();
                    if (CollectionUtils.isNotEmpty(pidList)) {
                        String pidListStr = StringUtils.join(pidList, ",");
                        DevPilotNotification.info(String.format("Found pid list: %s, delay max retry times", pidListStr == null ? "null" : pidListStr));
                        maxRetryTimes *= 3;
                    }
                }
            }

            return port != null && pid != null ? new Pair<>(port, pid) : findProcessAndPortByName();
        }
    }
    private Pair<Integer, Long> findProcessAndPortByName() {
        DevPilotNotification.info("try find process and port by name");
        List<Long> pidList = ProcessUtils.findDevPilotAgentPidList();
        String pidListStr = StringUtils.join(pidList, ",");
        DevPilotNotification.info(String.format("Found pid list: %s", pidListStr == null ? "null" : pidListStr));
        if (CollectionUtils.isNotEmpty(pidList)) {
            return new Pair<>(3000, pidList.get(0));
        } else {
            return ProcessUtils.isWindowsPlatform() ? new Pair<>(3000, 0L)  : null;
        }
    }

    public void killProcessAndDeleteInfoFile(Long pid) {
        if (pid > 0L && ProcessUtils.isProcessAlive(pid)) {
            DevPilotNotification.info(String.format("%d is alive, try to kill", pid));
            ProcessUtils.killProcess(pid);
        }

        this.deleteInfoFile();
    }

    public void deleteInfoFile() {
        File homeDir = this.getHomeDir();
        if (homeDir != null) {
            File infoFile = new File(homeDir, ".info");
            if (infoFile.exists()) {
                try {
                    infoFile.delete();
                    DevPilotNotification.info("Delete .info file success.");
                } catch (Exception e) {
                    DevPilotNotification.warn("Delete .info file encountered exception" + e.getMessage());
                }
            }

        }
    }
}
