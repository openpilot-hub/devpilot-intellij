package com.zhongan.devpilot.treesitter;

import com.zhongan.devpilot.util.LanguageUtil;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSTree;
import org.treesitter.TreeSitterGo;
import org.treesitter.TreeSitterJava;
import org.treesitter.TreeSitterPython;

public class TreeSitterParser {
    private final TSLanguage language;
    private final static Map<String, TSLanguage> treeSitter = new ConcurrentHashMap<>();

    static {
        treeSitter.put("java", new TreeSitterJava());
        treeSitter.put("go", new TreeSitterGo());
        treeSitter.put("python", new TreeSitterPython());
    }

    public TreeSitterParser(TSLanguage language) {
        this.language = language;
    }

    public String clearRedundantWhitespace(String originCode, int position, String output) {
        if (language == null) {
            return output;
        }

        var result = new StringBuilder(output);
        while (result.length() != 0 && result.charAt(0) == ' ') {
            result.deleteCharAt(0);
            if (containsError(buildFullCode(originCode, position, result.toString()))) {
                return " " + result;
            }
        }

        return result.toString();
    }

    public String parse(String originCode, int position, String output) {
        if (!output.startsWith(" ")) {
            return parseInner(originCode, position, output);
        }

        // handle special case : start with several whitespace
        var noWhitespaceResult = parseInner(originCode, position, output.trim());
        var whitespaceResult = parseInner(originCode, position, " " + output.trim());

        var result = whitespaceResult.length() < noWhitespaceResult.length()
                ? noWhitespaceResult : whitespaceResult;

        return clearRedundantWhitespace(originCode, position, result);
    }

    private String parseInner(String originCode, int position, String output) {
        if (language == null) {
            return output;
        }

        var result = new StringBuilder(output);
        while (result.length() != 0) {
            if (containsError(buildFullCode(originCode, position, result.toString()))) {
                result.deleteCharAt(result.length() - 1);
            } else {
                return result.toString();
            }
        }

        return output;
    }

    private String buildFullCode(String originCode, int position, String output) {
        StringBuilder stringBuilder = new StringBuilder(originCode);
        stringBuilder.insert(position, output);
        return stringBuilder.toString();
    }

    private boolean containsError(String input) {
        var treeString = getTree(input).getRootNode().toString();
        return treeString.contains("ERROR")
                || treeString.contains("MISSING \"}\"")
                || treeString.contains("MISSING \")\"");
    }

    private TSTree getTree(String input) {
        var parser = new TSParser();
        parser.setLanguage(language);
        return parser.parseString(null, input);
    }

    public static TreeSitterParser getInstance(String extension) {
        var language = LanguageUtil.getLanguageByExtension(extension);

        if (language == null) {
            return new TreeSitterParser(null);
        }

        TSLanguage tsLanguage = treeSitter.get(language.getLanguageName().toLowerCase(Locale.ROOT));

        return new TreeSitterParser(tsLanguage);
    }
}
