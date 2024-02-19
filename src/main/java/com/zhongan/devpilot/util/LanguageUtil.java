package com.zhongan.devpilot.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LanguageUtil {

    public static final Set<String> JVM_PLATFORM_LANGUAGES = ImmutableSet.of(
        "Java", "Scala", "Groovy", "Kotlin"
    );

    public static final Language JAVA = new Language().setLanguageName("Java")
            .setFileExtensions(Collections.singletonList("java"))
            .setTestFrameworks(Collections.singletonList("JUnit4"))
            .setMockFrameworks(Collections.singletonList("Mockito"));

    @NotNull
    public static String getFileSuffixByLanguage(@NotNull String languageName) {
        Language language = ObjectUtils.defaultIfNull(getLanguageByName(languageName), JAVA);
        return DOT + language.getDefaultFileExtension();
    }

    @Nullable
    public static Language getLanguageByName(@Nullable String languageName) {
        if (languageName == null) {
            return null;
        }
        return LANG_MAPPINGS.get(languageName.toLowerCase());
    }

    @Nullable
    public static Language getLanguageByExtension(@Nullable String extensionName) {
        if (extensionName == null) {
            return null;
        }
        extensionName = StringUtils.removeStart(extensionName, DOT);
        return EXT_MAPPINGS.get(extensionName);
    }

    private static final String DOT = ".";
    
    private static final Map<String, Language> LANG_MAPPINGS;
    
    private static final Map<String, Language> EXT_MAPPINGS;

    static {
        try {
            Map<String, Language> langMappings = new HashMap<>();
            Map<String, Language> extMappings = new HashMap<>();
            ObjectMapper objectMapper = new ObjectMapper();
            URL resource = LanguageUtil.class.getResource("/languageMappings.json");
            List<Language> languages = objectMapper.readValue(resource, new TypeReference<>() { });
            for (Language language : languages) {
                langMappings.put(language.getLanguageName().toLowerCase(), language);
                List<String> fileExtensions = new ArrayList<>();
                for (String fileExtension : language.getFileExtensions()) {
                    String extensionWithoutDot = StringUtils.removeStart(fileExtension, DOT);
                    fileExtensions.add(extensionWithoutDot);
                    extMappings.putIfAbsent(extensionWithoutDot, language);
                }
                language.setFileExtensions(Collections.unmodifiableList(fileExtensions));
            }
            LANG_MAPPINGS = Collections.unmodifiableMap(langMappings);
            EXT_MAPPINGS = Collections.unmodifiableMap(extMappings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Language {

        private String languageName;

        private List<String> fileExtensions;

        private List<String> testFrameworks;

        private List<String> mockFrameworks;

        @JsonIgnore
        public boolean isJvmPlatform() {
            return JVM_PLATFORM_LANGUAGES.contains(languageName);
        }

        @Nullable
        @JsonIgnore
        public String getDefaultFileExtension() {
            if (CollectionUtils.isEmpty(fileExtensions)) {
                return null;
            }
            return fileExtensions.get(0);
        }

        @Nullable
        @JsonIgnore
        public String getDefaultTestFramework() {
            if (CollectionUtils.isEmpty(testFrameworks)) {
                return null;
            }
            return testFrameworks.get(0);
        }

        @Nullable
        @JsonIgnore
        public String getDefaultMockFramework() {
            if (CollectionUtils.isEmpty(mockFrameworks)) {
                return null;
            }
            return mockFrameworks.get(0);
        }

        public String getLanguageName() {
            return languageName;
        }

        public Language setLanguageName(String languageName) {
            this.languageName = languageName;
            return this;
        }

        public List<String> getFileExtensions() {
            return fileExtensions;
        }

        public Language setFileExtensions(List<String> fileExtensions) {
            this.fileExtensions = fileExtensions;
            return this;
        }

        public List<String> getTestFrameworks() {
            return testFrameworks;
        }

        public Language setTestFrameworks(List<String> testFrameworks) {
            this.testFrameworks = testFrameworks;
            return this;
        }

        public List<String> getMockFrameworks() {
            return mockFrameworks;
        }

        public Language setMockFrameworks(List<String> mockFrameworks) {
            this.mockFrameworks = mockFrameworks;
            return this;
        }
        
    }

}
