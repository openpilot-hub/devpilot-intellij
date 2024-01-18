package com.zhongan.devpilot.util;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class MarkdownUtil {

    public static String getFileExtensionFromLanguage(String language) {
        return LanguageUtil.getFileSuffixByLanguage(language);
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

    public static String extractContents(String codeBlock) {
        List<String> blocks = divideMarkdown(codeBlock);
        List<String> contents = new ArrayList<>();
        for (String block : blocks) {
            if (block.startsWith("```")) {
                com.vladsch.flexmark.util.ast.Document parse = Parser.builder().build().parse(codeBlock);
                FencedCodeBlock codeNode = (FencedCodeBlock) parse.getChildOfType(FencedCodeBlock.class);
                if (codeNode == null) {
                    return null;
                }
                contents.add(codeNode.getContentChars().unescape().replaceAll("\\n$", ""));
            }
        }
        if (CollectionUtils.isEmpty(contents)) {
            return codeBlock;
        }
        return StringUtils.join(contents, "\n\n");
    }

}
