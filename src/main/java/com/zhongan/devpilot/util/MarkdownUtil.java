package com.zhongan.devpilot.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.zhongan.devpilot.gui.toolwindows.components.TextContentRenderer;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MarkdownUtil {

    public static String getFileExtensionFromLanguage(String language) {
        return languageFileExtMap.getOrDefault(language.toLowerCase(), ".txt");
    }

    /**
     * Splits a Markdown text into code and non-code blocks
     */
    public static List<String> divideMarkdown(String markdownContent) {
        List<String> blocks = new ArrayList<>();
        Pattern codeBlockPattern = Pattern.compile("(?s)```.*?```");
        Matcher codeBlockMatcher = codeBlockPattern.matcher(markdownContent);
        int previousEnd = 0;
        while (codeBlockMatcher.find()) {
            blocks.add(markdownContent.substring(previousEnd, codeBlockMatcher.start()));
            blocks.add(codeBlockMatcher.group());
            previousEnd = codeBlockMatcher.end();
        }
        blocks.add(markdownContent.substring(previousEnd));
        return blocks.stream()
            .filter(section -> !section.isBlank())
            .collect(Collectors.toList());
    }

    public static String textContent2Html(String markdownText) {
        MutableDataSet options = new MutableDataSet();
        Document document = Parser.builder(options).build().parse(markdownText);
        return HtmlRenderer.builder(options)
                .nodeRendererFactory(new TextContentRenderer.Factory())
                .build()
                .render(document);
    }

    private static final Map<String, String> languageFileExtMap = buildLanguageFileExtMap();

    private static Map<String, String> buildLanguageFileExtMap() {
        Map<String, String> languageFileExtMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        List<LanguageFileExtInfo> languageFileExtInfos;
        try {
            URL resource = MarkdownUtil.class.getResource("/languageMappings.json");
            languageFileExtInfos = objectMapper.readValue(resource, new TypeReference<>() {
            });
            for (LanguageFileExtInfo languageFileExtInfo : languageFileExtInfos) {
                languageFileExtMap.put(languageFileExtInfo.getLanguageName().toLowerCase(),
                    languageFileExtInfo.getFileExtensions().get(0));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return languageFileExtMap;
    }

    public static class LanguageFileExtInfo {

        private String languageName;

        private List<String> fileExtensions;

        public String getLanguageName() {
            return languageName;
        }

        public void setLanguageName(String languageName) {
            this.languageName = languageName;
        }

        public List<String> getFileExtensions() {
            return fileExtensions;
        }

        public void setFileExtensions(List<String> fileExtensions) {
            this.fileExtensions = fileExtensions;
        }

    }

}
