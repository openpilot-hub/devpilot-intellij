package com.zhongan.devpilot.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PromptTemplateTest {

    @Test
    public void testEmptyPromptTemplate() {
        PromptTemplate promptTemplate = PromptTemplate.empty();
        String result = promptTemplate.getPrompt();
        assertEquals("", result);
    }

    @Test
    public void testNonEmptyPromptTemplate() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello {{name}}!");
        promptTemplate.setVariable("name", "John");
        String result = promptTemplate.getPrompt();
        assertEquals("Hello John!", result);
    }

    @Test
    public void testNullPromptTemplate() {
        PromptTemplate promptTemplate = PromptTemplate.of(null);
        String result = promptTemplate.getPrompt();
        assertEquals("", result);
    }

    @Test
    public void testSetVariable() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello {{name}}!");
        promptTemplate.setVariable("name", "John");
        String result = promptTemplate.getPrompt();
        assertEquals("Hello John!", result);
    }

    @Test
    public void testSetNullVariableName() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello {{name}}!");
        promptTemplate.setVariable(null, "John");
        String result = promptTemplate.getPrompt();
        assertEquals("Hello {{name}}!", result);
    }

    @Test
    public void testSetNullVariableValue() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello {{name}}!");
        promptTemplate.setVariable("name", null);
        String result = promptTemplate.getPrompt();
        assertEquals("Hello {{name}}!", result);
    }

    @Test
    public void testAppendLast() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello");
        promptTemplate.appendLast(" {{name}}!");
        promptTemplate.setVariable("name", "John");
        String result = promptTemplate.getPrompt();
        assertEquals("Hello John!", result);
    }

    @Test
    public void testAppendNullLast() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello");
        promptTemplate.appendLast(null);
        String result = promptTemplate.getPrompt();
        assertEquals("Hello", result);
    }

    @Test
    public void testDefaultValue() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello {{name:John}}!");
        String result = promptTemplate.getPrompt();
        assertEquals("Hello John!", result);
    }

    @Test
    public void testEscapeVariable() {
        PromptTemplate promptTemplate = PromptTemplate.of("Hello ~{{name:John}}!");
        String result = promptTemplate.getPrompt();
        assertEquals("Hello {{name:John}}!", result);
    }

}