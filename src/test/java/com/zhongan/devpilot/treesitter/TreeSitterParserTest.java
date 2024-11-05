package com.zhongan.devpilot.treesitter;

import org.junit.Assert;
import org.junit.Test;

public class TreeSitterParserTest {
    @Test
    public void testJavaParse() {
        var wholeFile = "package org.example;\n" +
                "\n" +
                "public class AgentTest {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"\");\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n";
        var position = 120;
        var insertCode = "AgentTest completed.\");";

        var parser = TreeSitterParser.getInstance("java");
        var result = parser.parse(wholeFile, position, insertCode);

        Assert.assertEquals(result, "AgentTest completed.");

        wholeFile = "package org.example;\n" +
                "\n" +
                "public class AgentTest {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"\");\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n";
        insertCode = "      AgentTest completed.\");";

        parser = TreeSitterParser.getInstance("java");
        result = parser.parse(wholeFile, position, insertCode);

        Assert.assertEquals(result, "AgentTest completed.");

        wholeFile = "package org.example;\n" +
                "\n" +
                "public class AgentTest {\n" +
                "    public static void main(String[] args) {\n" +
                "        String" +
                "        System.out.println(\"AgentTest completed\");\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n";
        position = 106;
        insertCode = "    str = \"AgentTest\";";

        parser = TreeSitterParser.getInstance("java");
        result = parser.parse(wholeFile, position, insertCode);

        Assert.assertEquals(result, "str = \"AgentTest\";");

        wholeFile = "package org.example;\n" +
                "\n" +
                "public class AgentTest {\n" +
                "    public static void main(String[] args) {\n" +
                "        Sys" +
                "        System.out.println(\"AgentTest completed2\");\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "\n";
        position = 103;
        insertCode = "  tem.out.println(\"AgentTest completed1\");";

        parser = TreeSitterParser.getInstance("java");
        result = parser.parse(wholeFile, position, insertCode);

        Assert.assertEquals(result, "tem.out.println(\"AgentTest completed1\");");
    }

    @Test
    public void testPythonParse() {
        var wholeFile = "import pandas as pd\n" +
                "\n" +
                "if __name__ == '__main__':\n" +
                "    df = pd.read_excel(r'test.xlsx')\n" +
                "    for idx, row in tqdm(df.iterrows(), t ):\n" +
                "\n" +
                "\n" +
                "    print('done')";
        var position = 126;
        var insertCode = "otal=df.shape[0]):";

        var parser = TreeSitterParser.getInstance("py");
        var result = parser.parse(wholeFile, position, insertCode);

        Assert.assertEquals(result, "otal=df.shape[0]");
    }
}
