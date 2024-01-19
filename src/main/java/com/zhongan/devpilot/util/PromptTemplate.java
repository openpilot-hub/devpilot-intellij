package com.zhongan.devpilot.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.matcher.StringMatcher;
import org.apache.commons.text.matcher.StringMatcherFactory;

/**
 * Convenient prompt tool.
 * <p>
 * e.g.
 * <pre>
 *   PromptTemplate promptTemplate = PromptTemplate.of("Hello {{name}}!");
 *   promptTemplate.setVariable("name", "John");
 *   String result = promptTemplate.getPrompt();
 *   assertEquals("Hello John!", result);
 *
 *   PromptTemplate promptTemplate = PromptTemplate.of("Hello {{name:John}}!");
 *   String result = promptTemplate.getPrompt();
 *   assertEquals("Hello John!", result);
 * </pre>
 */
public class PromptTemplate {

    private static final char ESCAPE_CHARACTER = '~';
    
    private static final StringMatcher VAR_PREFIX = StringMatcherFactory.INSTANCE.stringMatcher("{{");
    
    private static final StringMatcher VAR_SUFFIX = StringMatcherFactory.INSTANCE.stringMatcher("}}");
    
    private static final StringMatcher DEFAULT_VALUE_DELIMITER = StringMatcherFactory.INSTANCE.stringMatcher(":");

    private final StringBuilder template;
    
    private final Map<String, Object> variables = new HashMap<>();

    PromptTemplate() {
        this.template = new StringBuilder();
    }

    PromptTemplate(String template) {
        this.template = new StringBuilder(template);
    }

    public static PromptTemplate empty() {
        return new PromptTemplate();
    }

    public static PromptTemplate of(String promptTemplate) {
        if (promptTemplate == null) {
            return empty();
        }
        return new PromptTemplate(promptTemplate);
    }

    public PromptTemplate setVariable(String name, String value) {
        if (name != null && value != null) {
            variables.put(name, value);
        }
        return this;
    }

    public PromptTemplate appendLast(String text) {
        if (text != null) {
            template.append(text);
        }
        return this;
    }

    public String getPrompt() {
        return new StringSubstitutor(variables)
                .setVariablePrefixMatcher(VAR_PREFIX)
                .setVariableSuffixMatcher(VAR_SUFFIX)
                .setValueDelimiterMatcher(DEFAULT_VALUE_DELIMITER)
                .setEscapeChar(ESCAPE_CHARACTER)
                .replace(template.toString());
    }

}
