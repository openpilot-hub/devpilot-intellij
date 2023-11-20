package com.zhongan.devpilot.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataSet;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MarkdownUtil {

    public static String mark2Html(String markdownText) {
        MutableDataSet options = new MutableDataSet();
        Document document = Parser.builder(options).build().parse(markdownText);
        return HtmlRenderer.builder(options)
            .nodeRendererFactory(new ContentNodeRenderer.Factory())
            .build()
            .render(document);
    }

    /**
     * To split a markdown text into code blocks and non-code blocks
     */
    public static List<String> splitBlocks(String markdownText) {
        List<String> blocks = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?s)```.*?```");
        Matcher matcher = pattern.matcher(markdownText);
        int start = 0;
        while (matcher.find()) {
            blocks.add(markdownText.substring(start, matcher.start()));
            blocks.add(matcher.group());
            start = matcher.end();
        }
        blocks.add(markdownText.substring(start));
        return blocks.stream()
            .filter(item -> !item.isBlank())
            .collect(Collectors.toList());
    }

    private static final Map<String, String> languageFileExtMap = buildLanguageFileExtMap();

    private static Map<String, String> buildLanguageFileExtMap() {
        Map<String, String> languageFileExtMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        List<LanguageFileExtInfo> languageFileExtInfos;
        try {
            URL resource = MarkdownUtil.class.getResource("/languageFileExtensionMappings.json");
            languageFileExtInfos = objectMapper.readValue(resource, new TypeReference<>() {
            });
            for (LanguageFileExtInfo languageFileExtInfo : languageFileExtInfos) {
                languageFileExtMap.put(languageFileExtInfo.getName().toLowerCase(),
                    languageFileExtInfo.getExtensions().get(0));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return languageFileExtMap;
    }

    public static String getFileExtensionFromLanguage(String language) {
        return languageFileExtMap.getOrDefault(language.toLowerCase(), ".txt");
    }

    public static class LanguageFileExtInfo {

        private String name;

        private String type;

        private List<String> extensions;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getExtensions() {
            return extensions;
        }

        public void setExtensions(List<String> extensions) {
            this.extensions = extensions;
        }

    }

}
