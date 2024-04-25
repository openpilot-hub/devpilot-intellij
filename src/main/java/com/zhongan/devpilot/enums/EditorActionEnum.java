package com.zhongan.devpilot.enums;

import java.util.Objects;

public enum EditorActionEnum {
    PERFORMANCE_CHECK("devpilot.action.performance.check", "Performance check in the following code",
            "Perform a performance check on the specified code. Only identify and report on the aspects where actual performance issues are found. Do not list out aspects that do not have issues.\n" +
                    "The check may focus on, but is not limited to, the following aspects:\n" +
                    "1.Algorithmic Efficiency: Check for inefficient algorithms that may slow down the program.\n" +
                    "2.Data Structures: Evaluate the use of data structures for potential inefficiencies.\n" +
                    "3.Loops: Inspect loops for unnecessary computations or operations that could be moved outside.\n" +
                    "4.Method Calls: Look for frequent method calls.\n" +
                    "5.Object Creation: Check for unnecessary object creation.\n" +
                    "6.Use of Libraries: Review the use of library methods for potential inefficiencies.\n" +
                    "7.Concurrency: If multithreading is used, ensure efficient operation and absence of bottlenecks or deadlocks.\n" +
                    "8.I/O Operations: Look for inefficient use of I/O operations.\n" +
                    "9.Database Queries: If the code interacts with a database, check for inefficient or excessive queries.\n" +
                    "10.Network Calls: If the code makes network calls, consider their efficiency and potential impact on performance.\n" +
                    "Remember, the goal of a performance check is to identify potential performance issues in the code, not to optimize every single detail. Always measure before and after making changes to see if the changes had the desired effect.\n" +
                    "\n" +
                    "The following code is being analyzed for performance: <code>\n" +
                    "{{selectedCode}} </code>\n" +
                    "For each identified issue, provide a report in the following format:\n" +
                    "\n" +
                    "### performance review report\n" +
                    "1. **the specific problem summary**\n" +
                    "   - Code: [Provide the problematic code snippet or a range of lines in the code where the issue was found.]\n" +
                    "   - Problem: [Describe the specific problem]\n" +
                    "   - Suggestion: [Provide a suggestion for fixing the issue]"
    ),

    GENERATE_COMMENTS("devpilot.action.generate.comments", "Generate comments in the following code",
            "Write inline comments for the key parts of the specified function.\n" +
                    "The comments should explain what each part of the function does in a clear and concise manner.\n" +
                    "Avoid commenting on every single line of code, as this can make the code harder to read and maintain. Instead, focus on the parts of the function that are complex, important, or not immediately obvious.\n" +
                    "Remember, the goal of inline comments is to help other developers understand the code, not to explain every single detail.\n\n" +
                    "The comment is being written for the following code: <code>\n" +
                    "{{selectedCode}} </code>"),

    GENERATE_METHOD_COMMENTS("devpilot.action.generate.method.comments", "",
            "Write a function comment for the specified function in the appropriate style for the programming language being used.\n" +
                    "The comment should start with a brief summary of what the function does. This should be followed by a detailed description, if necessary.\n" +
                    "Then, document each parameter, explaining the purpose of each one.\n" +
                    "If the function returns a value, describe what the function returns.\n" +
                    "If the function throws exceptions, document each exception and under what conditions it is thrown.\n" +
                    "Make sure the comment is clear, concise, and free of spelling and grammar errors.\n" +
                    "The comment should help other developers understand what the function does, how it works, and how to use it.\n" +
                    "Please note that the function definition is not included in this task, only the function comment is required.\n\n" +
                    "The comment is being written for the following code: <code> \n" +
                    "{{selectedCode}} </code>"),

    GENERATE_TESTS("devpilot.action.generate.tests", "Generate Tests in the following code",
        "{{selectedCode}}\nGiving the {{language:unknown}} code above, " +
            "please help to generate {{testFramework:suitable}} test cases for it, " +
            "mocking test data with {{mockFramework:suitable mock framework}} if necessary, " +
            "{{additionalMockPrompt:}}" +
            "be aware that if the code is untestable, " +
            "please state it and give suggestions instead."),

    FIX_THIS("devpilot.action.fix", "Fix This in the following code",
            "Perform a code fix on the specified code. Only identify and make changes in the aspects where actual issues are found. Do not list out aspects that do not have issues. \n" +
                    "The fix may focus on, but is not limited to, the following aspects:\n" +
                    "1. Bug Fixes: Identify and correct any errors or bugs in the code. Ensure the fix doesn't introduce new bugs.\n" +
                    "2. Performance Improvements: Look for opportunities to optimize the code for better performance. This could involve changing algorithms, reducing memory usage, or other optimizations.\n" +
                    "3. Code Clarity: Make the code easier to read and understand. This could involve renaming variables for clarity, breaking up complex functions into smaller ones.\n" +
                    "4. Code Structure: Improve the organization of the code. This could involve refactoring the code to improve its structure, or rearranging code for better readability.\n" +
                    "5. Coding Standards: Ensure the code follows the agreed-upon coding standards. This includes naming conventions, comment style, indentation, and other formatting rules.\n" +
                    "6. Error Handling: Improve error handling in the code. The code should fail gracefully and not expose any sensitive information when an error occurs.\n" +
                    "Remember, the goal of a code fix is to improve the quality of the code and make it work correctly, efficiently, and in line with the requirements. Always test the code after making changes to ensure it still works as expected.\n" +
                    "\n" +
                    "The following code is being fixed: <code>\n" +
                    "{{selectedCode}} </code>\n" +
                    "For each identified issue, provide an explanation of the problem and describe how it will be fixed. Then, present the fixed code."),

    REVIEW_CODE("devpilot.action.review", "Review code in the following code",
            "Perform a code review on the specified code. Only identify and report on the aspects where actual issues are found. Do not list out aspects that do not have issues.\n" +
                    "The review may focus on, but is not limited to, the following aspects:\n" +
                    "1. Code Clarity: Is the code easy to read and understand?\n" +
                    "2. Code Structure: Is the code well-structured and organized?\n" +
                    "3. Coding Standards: Does the code follow the agreed-upon coding standards?\n" +
                    "4. Error Handling: Does the code handle errors gracefully?\n" +
                    "5. Logic Errors: Are there any obvious mistakes in the code?\n" +
                    "6. Security: Are there any potential security vulnerabilities in the code?\n" +
                    "Remember, the goal of a code review is to improve the quality of the code and catch bugs before the code is executed. Always provide constructive feedback and explain why a change might be necessary.\n" +
                    "\n" +
                    "The following code is being reviewed: <code>\n" +
                    "{{selectedCode}} </code>\n" +
                    "For each identified issue, provide a report in the following format:\n" +
                    "\n" +
                    "### code review report" +
                    "1. **the specific problem summary**\n" +
                    "   - Code: [Provide the problematic code snippet or a range of lines in the code where the issue was found.]\n" +
                    "   - Problem: [Describe the specific problem]\n" +
                    "   - Suggestion: [Provide a suggestion for fixing the issue]"),

    EXPLAIN_THIS("devpilot.action.explain", "Explain this in the following code",
            "Write a detailed explanation for the specified code.\n" +
                    "Begin with a brief summary outlining its purpose and functionality.\n" +
                    "Then, dissect the code line by line or block by block.After each segment of code, provide a detailed explanation of its role and operation.\n" +
                    "Aim for clarity and conciseness in your explanation.Ensure that the explanation is comprehensive, covering all aspects of the code's operation, and free of spelling and grammar errors.\n" +
                    "The explanation should be interwoven with the code, providing a clear understanding of each part as it appears in the code sequence.\n" +
                    "Avoid including a separate copy of the complete original code, as the code is already explained in segments.\n\n" +
                    "The explanation is being written for the following code: <code> \n" +
                    "{{selectedCode}} </code>"),

    CODE_COMPLETIONS("devpilot.action.completions", "code completions",
            "You are a code completion service, please try to auto complete the code below at {{offsetCode}}, " +
            "only output the code, don't try to have conversation with user.```{{selectedCode}}```"
//                    +
//            "\nThe completion code returned is controlled within {{maxCompletionLength}} bytes within"
    );

    private final String label;

    private final String userMessage;

    private final String prompt;

    EditorActionEnum(String label, String userMessage, String prompt) {
        this.label = label;
        this.userMessage = userMessage;
        this.prompt = prompt;
    }

    public static EditorActionEnum getEnumByLabel(String label) {
        if (Objects.isNull(label)) {
            return null;
        }
        for (EditorActionEnum type : EditorActionEnum.values()) {
            if (type.getLabel().equals(label)) {
                return type;
            }
        }
        return null;
    }

    public String getLabel() {
        return label;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getUserMessage() {
        return userMessage;
    }
}
