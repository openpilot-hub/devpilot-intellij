package com.zhongan.devpilot.agents;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Contract;

public class BinaryManager {

    public static final BinaryManager INSTANCE = new BinaryManager();

    @Contract(pure = true)
    private BinaryManager() {
    }


    public synchronized void initBinary(File homeDir) throws Exception {
        File zipTmpDir = new File(System.getProperty("java.io.tmpdir"), String.format("devpilot_%d", System.currentTimeMillis()));
        try {
            File archDir = unZipBinary(zipTmpDir.getAbsolutePath());
            if (archDir == null || !archDir.exists()) {
                DevPilotNotification.warn("fail to unzip binary for " + VALID_ARCH);
                return;
            }

            File versionDir = archDir.getParentFile();

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
            DevPilotNotification.debug("Error occurred while initial file.");
        } finally {
            try {
                FileUtils.deleteDirectory(zipTmpDir);
            } catch (IOException ex) {
                DevPilotNotification.debug("Error occurred while deleting file.");
            }
        }
    }

    public File unZipBinary(String destDirPath) throws Exception {
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

    public static File unzipFile(InputStream stream, Path targetDir) {
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
                    if (parts[parts.length - 1].equalsIgnoreCase(VALID_ARCH)) {
                        finalDir = targetPath.toFile();
                    }
                } else {
                    handleFile(targetPath, zis);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error unzipping file", e);
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

    public static final String VALID_ARCH;

    static {
        VALID_ARCH = String.format("%s_%s", getSystemArch(), getPlatformName());
    }

    private static boolean needUnzip(String entryName) {
        return entryName.equalsIgnoreCase(VALID_ARCH) || entryName.equals("extension");
    }

    private static String getSystemArch() {
        String arch = CpuArch.is32Bit() ? "i686" : "x86_64";
        if ("aarch64".equals(System.getProperty("os.arch"))) {
            arch = "aarch64";
        }
        return arch;
    }

    private static String getPlatformName() {
        String platform;
        if (SystemInfo.isWindows) {
            platform = "windows";
        } else if (SystemInfo.isMac) {
            platform = "darwin";
        } else {
            if (!SystemInfo.isLinux) {
                throw new RuntimeException("DevPilot only supports platform Windows, macOS, Linux");
            }
            platform = "linux";
        }
        return platform;
    }
}
