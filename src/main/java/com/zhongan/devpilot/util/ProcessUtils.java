package com.zhongan.devpilot.util;

import com.intellij.openapi.diagnostic.Logger;
import com.zhongan.devpilot.agents.BinaryManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import static com.zhongan.devpilot.agents.BinaryManager.EXECUTABLE_NAME;

public class ProcessUtils {
    private static final Logger log = Logger.getInstance(ProcessUtils.class);

    public static final String WINDOWS_OS = "win";

    public static final String LINUX_OS = "nux";

    public static final String NIX_OS = "nix";

    public static final String MAC_OS = "mac";

    private ProcessUtils() {
    }

    public static String getWindowsCmdCommand() {
        String cmdPath = "cmd.exe"; // 默认值
        File cmdFile = new File(System.getenv("WINDIR") + "\\System32\\cmd.exe");
        if (!cmdFile.exists()) {
            // 尝试另一个常见位置
            cmdFile = new File("c:\\Windows\\System32\\cmd.exe");
        }

        if (cmdFile.exists()) {
            cmdPath = cmdFile.getAbsolutePath();
        }
        return cmdPath;
    }

    public static boolean isProcessAlive(long pid) {
        String osName = System.getProperty("os.name").toLowerCase();
        String command;
        if (isWindowsPlatform()) {
            log.info(String.format("Check alive Windows mode. Pid: [%d]", pid));
            try {
                // 使用WMI查询更可靠
                String[] commands = {getWindowsCmdCommand(), "/c", "wmic process where ProcessId=" + pid + " get ProcessId /format:list"};
                Process pr = Runtime.getRuntime().exec(commands);
                BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("ProcessId=" + pid)) {
                        return true;
                    }
                }
                return false;
            } catch (Exception ex) {
                log.warn("检查进程状态异常", ex);
                return false;
            }
        } else {
            if (!osName.contains(NIX_OS) && !osName.contains(LINUX_OS) && !osName.contains(MAC_OS)) {
                log.info(String.format("Unsupported OS: Check alive for Pid: [%d] return false", pid));
                return false;
            }

            log.info(String.format("Check alive Linux/Unix mode. Pid: [%d]", pid));
            command = "ps -p " + pid;
            return isProcessIdRunning(pid, command);
        }
    }

    public static boolean isWindowsPlatform() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains(WINDOWS_OS);
    }

    public static void killProcess(long pid) {
        log.info("Kill devpilot-agents process: " + pid);
        Runtime rt = Runtime.getRuntime();
        String osName = System.getProperty("os.name").toLowerCase();

        try {
            Process process = null;
            if (isWindowsPlatform()) {
                log.info(String.format("Kill process in Windows mode. Pid: [%d]", pid));
                process = rt.exec("taskkill /PID " + pid);
                boolean terminated = process.waitFor(3, TimeUnit.SECONDS);

                if (!terminated || isProcessAlive(pid)) {
                    log.info(String.format("Process %d is still alive，Try to kill force.", pid));
                    process = rt.exec("taskkill /F /T /PID " + pid);
                    terminated = process.waitFor(5, TimeUnit.SECONDS);

                    if (!terminated || isProcessAlive(pid)) {
                        log.warn(String.format("Failed to kill %d", pid));
                    }
                }
            } else if (!osName.contains(NIX_OS) && !osName.contains(LINUX_OS) && !osName.contains(MAC_OS)) {
                log.info(String.format("Unsupported OS: Check alive for Pid: [%d] return false", pid));
            } else {
                log.info(String.format("Kill process in Linux/Unix mode. Pid: [%d]", pid));
                process = rt.exec("kill " + pid);
            }

            if (process != null) {
                process.waitFor(5L, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Kill process encountered exception");
        }

    }

    public static List<Long> findDevPilotAgentPidList(long pid) {
        if (pid > 0) {
            List<Long> pidList = new ArrayList<>();
            pidList.add(pid);
            return pidList;
        }
        String version = BinaryManager.INSTANCE.getVersion();
        if (StringUtils.isEmpty(version)) {
            return Collections.emptyList();
        }
        boolean fromSources = ProjectUtil.isSandboxProject();
        return getPidListFromName(BinaryManager.INSTANCE.getIdeType() + File.separator + (fromSources ? ("sandbox" + File.separator) : StringUtils.EMPTY) + "bin" + File.separator + BinaryManager.INSTANCE.getVersion() + File.separator + BinaryManager.INSTANCE.getCompatibleArch() + File.separator + EXECUTABLE_NAME);
    }

    private static boolean isProcessIdRunning(long pid, String command) {
        log.info(String.format("Command [%s]", command));

        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);
            InputStreamReader isReader = new InputStreamReader(pr.getInputStream());
            BufferedReader bReader = new BufferedReader(isReader);

            String strLine;
            do {
                if ((strLine = bReader.readLine()) == null) {
                    return false;
                }
            } while (!strLine.contains(pid + " "));

            return true;
        } catch (Exception ex) {
            log.warn(String.format("Got exception using system command [%s].", command), ex);
            return true;
        }
    }

    private static List<Long> getPidListFromName(String name) {
        String osName = System.getProperty("os.name").toLowerCase();
        String[] command;
        List<Long> pids;
        if (isWindowsPlatform()) {
            log.info(String.format("Get pid list Windows mode. Name: [%s]", name));
            String cmdCommandPath = getWindowsCmdCommand();
            command = new String[]{cmdCommandPath, "/c", "tasklist /FI \"IMAGENAME eq " + name + ".exe\""};
            pids = getPidListWindows(command);
            if (pids == null || pids.isEmpty()) {
                command = new String[]{cmdCommandPath, "/c", "c:\\windows\\System32\\tasklist.exe /FI \"IMAGENAME eq " + name + ".exe\""};
                pids = getPidListWindows(command);
                if (pids == null || pids.isEmpty()) {
                    command = new String[]{cmdCommandPath, "/c", "wmic process where \"name='" + name + "'\" get processid"};
                    return getPidListByWmic(command);
                }
            }

            return pids;
        } else if (!osName.contains(NIX_OS) && !osName.contains(LINUX_OS)) {
            if (!osName.contains(MAC_OS)) {
                log.info(String.format("Unsupported OS: Get pid list for Name: [%s] return empty list", name));
                return null;
            } else {
                log.info(String.format("Get pid list MacOS mode. Name: [%s]", name));
                command = new String[]{"/bin/sh", "-c", "ps -eo pid,command | grep " + name};
                pids = getPidList(command);
                if (pids == null || pids.isEmpty()) {
                    command = new String[]{"pgrep", name};
                    pids = getPidListByGrep(command);
                }

                return pids;
            }
        } else {
            log.info(String.format("Get pid list Linux/Unix mode. Name: [%s]", name));
            command = new String[]{"/bin/sh", "-c", "ps -eo pid,command | grep " + name};
            return getPidList(command);
        }
    }

    private static List<Long> getPidList(String[] command) {
        log.info(String.format("getPidList Command [%s]", String.join(",", command)));

        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);
            InputStreamReader isReader = new InputStreamReader(pr.getInputStream());
            BufferedReader bReader = new BufferedReader(isReader);
            List<Long> pidList = new ArrayList<>();

            String strLine;
            while ((strLine = bReader.readLine()) != null) {
                if (strLine.contains(EXECUTABLE_NAME)) {
                    String[] outputs = strLine.trim().split("\\s+");
                    if (outputs.length > 0) {
                        try {
                            pidList.add(Long.parseLong(outputs[0]));
                        } catch (Exception e) {
                            log.warn(String.format("Parse [%s] and add pid list encountered exception: %s", strLine, e.getMessage()));
                        }
                    }
                }
            }

            return pidList;
        } catch (Exception ex) {
            log.warn(String.format("Got exception using system command [%s].", String.join(",", command)), ex);
            return null;
        }
    }

    private static List<Long> getPidListWindows(String[] command) {
        log.info(String.format("getPidListWindows Command [%s]", String.join(",", command)));

        try {
            Runtime rt = Runtime.getRuntime();
            Process pr = rt.exec(command);
            InputStreamReader isReader = new InputStreamReader(pr.getInputStream());
            BufferedReader bReader = new BufferedReader(isReader);
            List<Long> pidList = new ArrayList<>();

            String strLine;
            while ((strLine = bReader.readLine()) != null) {
                log.info("windows get pid output:" + strLine);
                if (strLine.contains(EXECUTABLE_NAME)) {
                    String[] outputs = strLine.trim().split("[\\s\t]+");
                    if (outputs.length > 0) {
                        try {
                            pidList.add(Long.parseLong(outputs[1]));
                        } catch (Exception e) {
                            log.warn(String.format("Parse [%s] and add pid list encountered exception: %s", strLine, e.getMessage()));
                        }
                    }
                }
            }

            return pidList;
        } catch (Exception ex) {
            log.warn(String.format("Got exception using system command [%s].", String.join(",", command)), ex);
            return null;
        }
    }

    private static List<Long> getPidListByGrep(String[] command) {
        log.info(String.format("getPidListByGrep Command [%s]", String.join(",", command)));
        List<Long> pids = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    pids.add(Long.parseLong(line.trim()));
                } catch (Exception e) {
                    log.warn(String.format("Parse [%s] and add pid list encountered exception: %s", line, e.getMessage()));
                }
            }

            in.close();
            process.waitFor();
        } catch (Exception ex) {
            log.warn(String.format("Got exception using system command [%s].", String.join(",", command)), ex);
        }

        return pids;
    }

    private static List<Long> getPidListByWmic(String[] command) {
        log.info(String.format("getPidListByWmic Command [%s]", String.join(",", command)));
        List<Long> pids = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> lines = new ArrayList<>();

            String line;
            while ((line = in.readLine()) != null) {
                lines.add(line);
            }

            in.close();
            process.waitFor();
            if (lines.size() > 1) {
                try {
                    pids.add(Long.parseLong(lines.get(1).trim()));
                } catch (Exception e) {
                    log.warn(String.format("Parse [%s] and add pid list encountered exception: %s", lines, e.getMessage()));
                }
            }
        } catch (Exception ex) {
            log.warn(String.format("Got exception using system command [%s].", String.join(",", command)), ex);
        }
        return pids;
    }

    public static String getOSName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (isWindowsPlatform()) {
            osName = WINDOWS_OS;
        } else if (osName.contains(MAC_OS)) {
            osName = MAC_OS;
        } else if (osName.contains(LINUX_OS)) {
            osName = LINUX_OS;
        } else if (osName.contains(NIX_OS)) {
            osName = NIX_OS;
        }
        return osName;
    }
}
