package com.zhongan.devpilot.agents;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.zhongan.devpilot.util.ProcessUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.NotNull;

public class AgentsRunner {
    private static final Logger LOG = Logger.getInstance(AgentsRunner.class);

    public static final AgentsRunner INSTANCE = new AgentsRunner();

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static volatile AtomicBoolean initialRunning = new AtomicBoolean(false);

    private final List<AgentRefreshedObserver> refreshObservers = new ArrayList<>();

    public void addRefreshObserver(AgentRefreshedObserver observer) {
        refreshObservers.add(observer);
    }

    public void removeRefreshObserver(AgentRefreshedObserver observer) {
        refreshObservers.remove(observer);
    }

    public void triggerRefresh() {
        for (AgentRefreshedObserver observer : refreshObservers) {
            try {
                observer.onRefresh();
            } catch (Throwable e) {
                LOG.error("执行刷新观察者失败", e);
            }
        }
    }

    public synchronized CompletableFuture<Boolean> runAsync(boolean force) {
        initialRunning.set(true);
        try {
            return CompletableFuture.supplyAsync(() -> {
                File homeDir = BinaryManager.INSTANCE.getHomeDir();
                if (homeDir == null) {
                    LOG.warn("Home dir is null, skip running DevPilot-Agents.");
                    return false;
                }

                if (!force) {
                    BinaryManager.AgentCheckResult checkRes = BinaryManager.INSTANCE.checkIfAgentRunning(homeDir);
                    if (checkRes.isRunning()) {
                        LOG.info("Skip running DevPilot-Agents for already running.");
                        return true;
                    }
                }
                BinaryManager.INSTANCE.findProcessAndKill();

                boolean processRes = BinaryManager.INSTANCE.postProcessBeforeRunning(homeDir);
                if (!processRes) {
                    LOG.info("Skip running DevPilot-Agents for failure of init binary.");
                    return false;
                }
                boolean status = doRun(homeDir);
                if (status) {
                    triggerRefresh();
                }
                return status;
            });
        } finally {
            initialRunning.set(false);
        }
    }

    protected boolean doRun(File homeDir) {
        if (homeDir == null) {
            return false;
        }
        try {
            int port = getAvailablePort();
            List<String> commands = createCommand(BinaryManager.INSTANCE.getBinaryPath(homeDir), port);
            ProcessBuilder builder = new ProcessBuilder(commands);
            builder.directory(homeDir);

            Map<String, String> env = builder.environment();
            String pathVariableValue = PathEnvironmentVariableUtil.getPathVariableValue();
            if (StringUtils.isNotBlank(pathVariableValue)) {
                String currentPath = env.get("PATH");
                if (StringUtils.isNotBlank(currentPath)) {
                    env.put("PATH", pathVariableValue + File.pathSeparator + currentPath);
                } else {
                    env.put("PATH", pathVariableValue);
                }
            }

            LOG.info("启动命令: " + String.join(" ", commands));
            LOG.info("工作目录: " + homeDir.getAbsolutePath());
            LOG.info("环境变量PATH: " + env.get("PATH"));

            Process process = builder.start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LOG.info("Agent输出: " + line);
                        if (StringUtils.contains(line, "Server is listening on port")) {
                            LOG.warn("监听到Agent启动成功, 将触发刷新操作...");
                            triggerRefresh();
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("读取进程输出异常", e);
                }
            }).start();

            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LOG.warn("Agent错误: " + line);
                    }
                } catch (IOException e) {
                    LOG.warn("读取进程错误输出异常", e);
                }
            }).start();

            boolean aliveFlag = process.isAlive();
            if (aliveFlag) {
                try {
                    Thread.sleep(2000);
                    aliveFlag = process.isAlive();
                    if (!aliveFlag) {
                        LOG.warn("进程启动后立即退出，可能存在兼容性问题");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                long pid = NumberUtils.LONG_ZERO;
                try {
                    pid = process.pid();
                } catch (Exception e) {
                    LOG.warn("Error occurred while getting pid from process.", e);
                }
                writeInfoFile(homeDir, ProcessUtils.findDevPilotAgentPidList(pid), port);
            }
            return aliveFlag;
        } catch (Exception e) {
            LOG.warn("Failed to run DevPilot-Agents.", e);
            return false;
        }
    }

    protected int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            LOG.warn("No available port", e);
            throw new RuntimeException("No available port", e);
        }
    }

    public void writeInfoFile(File homeDir, List<Long> pids, int port) {
        if (homeDir != null) {
            File infoFile = new File(homeDir, BinaryManager.INSTANCE.getIdeInfoPath());
            try (FileWriter writer = new FileWriter(infoFile)) {
                writer.write(port + System.lineSeparator());
                for (Long pid : pids) {
                    writer.write(pid + System.lineSeparator());
                }
                LOG.info(String.format("Write info file to %s with port %s success.", homeDir.getName(), port));
            } catch (IOException e) {
                LOG.warn(String.format("Failed to write info file: %s.", homeDir.getName()), e);
            }
        }
    }

    protected List<String> createCommand(@NotNull String binaryPath, int port) {
        List<String> commands = new ArrayList<>();
        if (ProcessUtils.isWindowsPlatform()) {
            commands.add(ProcessUtils.getWindowsCmdCommand());
            commands.add("/c");
            commands.add("\"" + binaryPath + "\"");
        } else {
            commands.add(binaryPath);
        }
        commands.add("--port");
        commands.add(String.valueOf(port));

        LOG.info("Starting DevPilot-Agents with command: " + commands);
        return commands;
    }

    protected void delayKillOldProcess(Long pid) {
        executorService.schedule(() -> BinaryManager.INSTANCE.killOldProcess(pid), 30, TimeUnit.SECONDS);
    }

}
