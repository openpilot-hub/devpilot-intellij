package com.zhongan.devpilot.agents;

import com.intellij.openapi.diagnostic.Logger;
import com.zhongan.devpilot.util.ProcessUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class AgentsRunner {
    private static final Logger LOG = Logger.getInstance(AgentsRunner.class);

    public static final AgentsRunner INSTANCE = new AgentsRunner();

    public synchronized boolean run() throws Exception {
        File homeDir = BinaryManager.INSTANCE.getHomeDir();
        if (homeDir == null) {
            LOG.warn("Home dir is null, skip running DevPilot-Agents.");
            return false;
        }
        BinaryManager.INSTANCE.postProcessBeforeRunning(homeDir);

        int port = getAvailablePort();
        List<String> commands = createCommand(BinaryManager.INSTANCE.getBinaryPath(homeDir), port);
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.directory(homeDir);
        Process process = builder.start();

        boolean aliveFlag = process.isAlive();
        if (aliveFlag) {
            writeInfoFile(homeDir, ProcessUtils.findDevPilotAgentPidList(), port);
        }
        return aliveFlag;
    }

    private int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            LOG.warn("No available port", e);
            throw new RuntimeException("No available port", e);
        }
    }

    public void writeInfoFile(File homeDir, List<Long> pids, int port) {
        if (homeDir != null) {
            File infoFile = new File(homeDir, ".info");
            try (FileWriter writer = new FileWriter(infoFile)) {
                writer.write(port + System.lineSeparator());
                for (Long pid : pids) {
                    writer.write(pid + System.lineSeparator());
                }
                LOG.info(String.format("Write .info file to %s with port %s success.", homeDir.getName(), port));
            } catch (IOException e) {
                LOG.warn(String.format("Failed to write .info file: %s.", homeDir.getName()), e);
            }
        }
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

        LOG.info("Starting DevPilot-Agents with command: " + commands);
        return commands;
    }

}
