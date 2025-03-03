package com.zhongan.devpilot.embedding.scanner;

import com.intellij.openapi.diagnostic.Logger;
import com.zhongan.devpilot.embedding.enums.DevPilotFileType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IgnoreAwareScanner {
    private static final Logger log = Logger.getInstance(IgnoreAwareScanner.class);

    private final Set<String> ignorePatterns = new HashSet<>();

    private final String directory;

    private final List<DevPilotFileType> fileTypes;

    private final Map<DevPilotFileType, List<File>> typeFiles;

    public IgnoreAwareScanner(String directory, List<DevPilotFileType> fileTypes) {
        this.directory = directory;
        this.fileTypes = fileTypes;
        this.typeFiles = new HashMap<>();
        loadIgnorePatterns(directory);
    }

    private void loadIgnorePatterns(String directory) {
        try {
            File gitignoreFile = new File(directory, ".gitignore");
            if (gitignoreFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(gitignoreFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            ignorePatterns.add(line);
                        }
                    }
                }
            }
            File devPilotIgnoreFile = new File(directory, ".devpilotignore");
            if (devPilotIgnoreFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(devPilotIgnoreFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            ignorePatterns.add(line);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Exception occurred while loading ignore patterns.", e);
        }
    }

    private boolean shouldIgnore(File file) {
        if (file == null || file.getParentFile() == null) {
            return false;
        }
        String relativePath = getRelativePath(file);
        for (String pattern : ignorePatterns) {
            if (wildCardMatch(relativePath, file.getName(), pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean wildCardMatch(String filePath, String fileName, String pattern) {
        boolean equals = false;

        try {
            String normalizedFilePath = filePath.replace("\\", "/");
            String normalizedPattern = pattern.replace("\\", "/");
            if (!normalizedPattern.contains("*") && !normalizedPattern.contains("?")) {
                equals = normalizedFilePath.contains(normalizedPattern);
            } else {
                String regexPattern = pattern.replace(".", "\\.");
                regexPattern = regexPattern.replace("*", ".*");
                regexPattern = regexPattern.replace("?", "[1-9]");
                regexPattern = "^" + regexPattern + "$";
                Pattern regEx = Pattern.compile(regexPattern);
                Matcher matcher = regEx.matcher(fileName);
                equals = matcher.find();
            }
            return equals;
        } catch (Exception ex) {
            log.warn("failed to match rule " + pattern + " with file " + ex);
            return equals;
        }
    }

    private String getRelativePath(File file) {
        Path normalizedPath = file.toPath().normalize();
        return normalizedPath.toString().substring(directory.length() + 1);
    }

    public void scan() {
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            scanDirectory(dir);
        }
    }

    private void scanDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    scanDirectory(file);
                } else if (!shouldIgnore(file)) {
                    for (DevPilotFileType fileType : fileTypes) {
                        typeFiles.putIfAbsent(fileType, new ArrayList<>());
                        if (file.getName().endsWith(fileType.getExtension())) {
                            this.typeFiles.get(fileType).add(file);
                            break;
                        }
                    }
                }
            }
        }
    }

    public Map<DevPilotFileType, List<File>> getFiles() {
        return typeFiles;
    }
}