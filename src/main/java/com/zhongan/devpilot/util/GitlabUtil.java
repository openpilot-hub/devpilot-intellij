package com.zhongan.devpilot.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ini4j.Ini;
import org.ini4j.Profile;

public class GitlabUtil {

    // DevPilotNotification.error(GitlabUtil.getRemoteUrl(project, FileDocumentManager.getInstance().getFile(editor.getDocument())));

    public static String getRemoteUrl(Project project, VirtualFile file) {
        String url = null;
        try {
            String configFilePath = getConfigFilePath(project.getBasePath(), file.getPath());
            Ini ini = loadIni(configFilePath);
            url = getRemoteUrl(ini);
        } catch (IOException e) {
            DevPilotNotification.error("Error occurred while get remote url from config file" + e.getMessage());
        }
        return url;
    }

    private static String getConfigFilePath(String basePath, String filePath) {
        String configFilePath = basePath + "/.git/config";
        File file = new File(configFilePath);
        if (!file.exists()) {
            Path prefixPath = Paths.get(basePath).toAbsolutePath();
            Path filePathAbsolute = Paths.get(filePath).toAbsolutePath();
            Path relativePath = prefixPath.relativize(filePathAbsolute);
            Path targetPath = prefixPath.resolve(relativePath.subpath(0, 1));
            return getConfigFilePath(targetPath.toString(), filePath);
        }
        return configFilePath;
    }

    private static Ini loadIni(String configFilePath) throws IOException {
        Ini ini = new Ini();
        ini.load(new File(configFilePath));
        return ini;
    }

    private static String getRemoteUrl(Ini ini) {
        Pair<Collection<Remote>, Collection<Url>> pairs = parseRemotes(ini);
        Collection<Remote> remotes = pairs.getFirst();

        for (Remote remote : remotes) {
            if ("origin".equalsIgnoreCase(remote.getName())) {
                return remote.getUrls().iterator().next();
            }
        }

        Remote firstRemote = remotes.iterator().next();
        return firstRemote.getUrls().iterator().next();
    }

    private static Pair<Collection<Remote>, Collection<Url>> parseRemotes(Ini ini) {
        Collection<Remote> remotes = new ArrayList<>();
        Collection<Url> urls = new ArrayList<>();

        for (String sectionName : ini.keySet()) {
            Profile.Section section = ini.get(sectionName);

            Remote remote = parseRemote(sectionName, section);
            if (remote != null) {
                remotes.add(remote);
            } else {
                Url url = parseUrl(sectionName, section);
                if (url != null) {
                    urls.add(url);
                }
            }
        }

        return Pair.create(remotes, urls);
    }

    private static final Pattern REMOTE_SECTION = Pattern.compile("(?:svn-)?remote \"(.*)\"", Pattern.CASE_INSENSITIVE);

    private static final Pattern URL_SECTION = Pattern.compile("url \"(.*)\"", Pattern.CASE_INSENSITIVE);


    private static Remote parseRemote(String sectionName, Profile.Section section) {
        Matcher matcher = REMOTE_SECTION.matcher(sectionName);
        if (matcher.matches() && matcher.groupCount() == 1) {
            List<String> fetch = ContainerUtil.notNullize(section.getAll("fetch"));
            List<String> push = ContainerUtil.notNullize(section.getAll("push"));
            List<String> url = ContainerUtil.notNullize(section.getAll("url"));
            List<String> pushUrl = ContainerUtil.notNullize(section.getAll("pushurl"));
            return new Remote(matcher.group(1), fetch, push, url, pushUrl);
        }
        return null;
    }

    private static Url parseUrl(String sectionName, Profile.Section section) {
        Matcher matcher = URL_SECTION.matcher(sectionName);
        if (matcher.matches() && matcher.groupCount() == 1) {
            String insteadOf = section.get("insteadof");
            String pushInsteadOf = section.get("pushinsteadof");
            return new Url(matcher.group(1), insteadOf, pushInsteadOf);
        }
        return null;
    }

    private static final class Remote {

        private final String name;

        List<String> fetchSpecs;

        List<String> pushSpec;

        List<String> urls;

        List<String> pushUrls;

        private Remote(String name,
                       List<String> fetchSpecs,
                       List<String> pushSpec,
                       List<String> urls,
                       List<String> pushUrls) {
            this.name = name;
            this.fetchSpecs = fetchSpecs;
            this.pushSpec = pushSpec;
            this.urls = urls;
            this.pushUrls = pushUrls;
        }

        private Collection<String> getUrls() {
            return urls;
        }

        private Collection<String> getPushUrls() {
            return pushUrls;
        }

        private List<String> getPushSpec() {
            return pushSpec;
        }

        private List<String> getFetchSpecs() {
            return fetchSpecs;
        }

        private String getName() {
            return name;
        }

    }

    private static final class Url {
        private final String name;

        private final String insteadof;

        private final String pushInsteadof;

        private Url(String name, String insteadof, String pushInsteadof) {
            this.name = name;
            this.insteadof = insteadof;
            this.pushInsteadof = pushInsteadof;
        }

        public String getInsteadOf() {
            return insteadof;
        }

        public String getPushInsteadOf() {
            return pushInsteadof;
        }

    }

}
