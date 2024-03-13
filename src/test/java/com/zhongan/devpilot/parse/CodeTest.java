package com.zhongan.devpilot.parse;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeTest {
    @Test
    public void testCodeParse() {
        String input = "以下是一段程序 ```java\n def main(): pass\na = 10 \n b=100\n ``` ，该程序做了xxx动作";
        String regex = "```[^`]+?\\n+?(.*?)```";

        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            String content = matcher.group(1);
            System.out.println("提取到的content部分：" + content);
        } else {
            System.out.println("未找到匹配的内容");
        }
    }
}
