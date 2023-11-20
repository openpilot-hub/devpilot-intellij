package com.zhongan.devpilot.parse;

import com.zhongan.devpilot.util.MarkdownUtil;
import org.junit.Assert;
import org.junit.Test;

public class MarkdownTest {
    private static final String markdown = "The most commonly used fast sort algorithm in Java is the QuickSort algorithm. Here's an example implementation of the QuickSort algorithm in Java:\n" +
            "```java\n" +
            "public class QuickSort {\n" +
            "  \n" +
            "    public static void main(String[] args) {\n" +
            "        int[] arr = {64, 34, 25, 12, 22, 11, 90};\n" +
            "        int n = arr.length;\n" +
            " \n" +
            "        QuickSort ob = new QuickSort();\n" +
            "        ob.sort(arr, 0, n - 1);\n" +
            " \n" +
            "        System.out.println(\"Sorted array:\");\n" +
            "        for (int i : arr) {\n" +
            "            System.out.print(i + \" \");\n" +
            "        }\n" +
            "    }\n" +
            " \n" +
            "    private void sort(int[] arr, int low, int high) {\n" +
            "        if (low < high) {\n" +
            "            int pi = partition(arr, low, high);\n" +
            " \n" +
            "            sort(arr, low, pi - 1);\n" +
            "            sort(arr, pi + 1, high);\n" +
            "        }\n" +
            "    }\n" +
            " \n" +
            "    private int partition(int[] arr, int low, int high) {\n" +
            "        int pivot = arr[high];\n" +
            "        int i = (low - 1);\n" +
            " \n" +
            "        for (int j = low; j < high; j++) {\n" +
            "            if (arr[j] <= pivot) {\n" +
            "                i++;\n" +
            " \n" +
            "                int temp = arr[i];\n" +
            "                arr[i] = arr[j];\n" +
            "                arr[j] = temp;\n" +
            "            }\n" +
            "        }\n" +
            " \n" +
            "        int temp = arr[i + 1];\n" +
            "        arr[i + 1] = arr[high];\n" +
            "        arr[high] = temp;\n" +
            " \n" +
            "        return i + 1;\n" +
            "    }\n" +
            "}\n" +
            "```\n" +
            "This code will sort an array of integers using the QuickSort algorithm. The `sort` method recursively calls itself to divide the array into sub-arrays and the `partition` method selects a pivot element and rearranges the array such that all elements smaller than the pivot are on the left and all elements greater than the pivot are on the right.\n" +
            "\n" +
            "Note that there are other fast sorting algorithms available in Java, such as MergeSort and Arrays.sort() using a modified version of QuickSort called Dual-Pivot QuickSort.";

    @Test
    public void testMarkdownSpilt() {
        var list = MarkdownUtil.splitBlocks(markdown);

        var result = new String[]{
                "The most commonly used fast sort algorithm in Java is the QuickSort algorithm. Here's an example implementation of the QuickSort algorithm in Java:\n",
                "```java\n" +
                        "public class QuickSort {\n" +
                        "  \n" +
                        "    public static void main(String[] args) {\n" +
                        "        int[] arr = {64, 34, 25, 12, 22, 11, 90};\n" +
                        "        int n = arr.length;\n" +
                        " \n" +
                        "        QuickSort ob = new QuickSort();\n" +
                        "        ob.sort(arr, 0, n - 1);\n" +
                        " \n" +
                        "        System.out.println(\"Sorted array:\");\n" +
                        "        for (int i : arr) {\n" +
                        "            System.out.print(i + \" \");\n" +
                        "        }\n" +
                        "    }\n" +
                        " \n" +
                        "    private void sort(int[] arr, int low, int high) {\n" +
                        "        if (low < high) {\n" +
                        "            int pi = partition(arr, low, high);\n" +
                        " \n" +
                        "            sort(arr, low, pi - 1);\n" +
                        "            sort(arr, pi + 1, high);\n" +
                        "        }\n" +
                        "    }\n" +
                        " \n" +
                        "    private int partition(int[] arr, int low, int high) {\n" +
                        "        int pivot = arr[high];\n" +
                        "        int i = (low - 1);\n" +
                        " \n" +
                        "        for (int j = low; j < high; j++) {\n" +
                        "            if (arr[j] <= pivot) {\n" +
                        "                i++;\n" +
                        " \n" +
                        "                int temp = arr[i];\n" +
                        "                arr[i] = arr[j];\n" +
                        "                arr[j] = temp;\n" +
                        "            }\n" +
                        "        }\n" +
                        " \n" +
                        "        int temp = arr[i + 1];\n" +
                        "        arr[i + 1] = arr[high];\n" +
                        "        arr[high] = temp;\n" +
                        " \n" +
                        "        return i + 1;\n" +
                        "    }\n" +
                        "}\n" +
                        "```",
                "\nThis code will sort an array of integers using the QuickSort algorithm. The `sort` method recursively calls itself to divide the array into sub-arrays and the `partition` method selects a pivot element and rearranges the array such that all elements smaller than the pivot are on the left and all elements greater than the pivot are on the right.\n" +
                        "\n" +
                        "Note that there are other fast sorting algorithms available in Java, such as MergeSort and Arrays.sort() using a modified version of QuickSort called Dual-Pivot QuickSort."
        };

        Assert.assertArrayEquals(result, list.toArray());
    }

    @Test
    public void testMark2Html() {
        var result = MarkdownUtil.mark2Html(markdown);

        var expected = "<p style=\"margin-top: 5px; margin-bottom: 5px;\">The most commonly used fast sort algorithm in Java is the QuickSort algorithm. Here's an example implementation of the QuickSort algorithm in Java:</p>\n" +
                "<pre><code class=\"language-java\">public class QuickSort {\n" +
                "  \n" +
                "    public static void main(String[] args) {\n" +
                "        int[] arr = {64, 34, 25, 12, 22, 11, 90};\n" +
                "        int n = arr.length;\n" +
                " \n" +
                "        QuickSort ob = new QuickSort();\n" +
                "        ob.sort(arr, 0, n - 1);\n" +
                " \n" +
                "        System.out.println(&quot;Sorted array:&quot;);\n" +
                "        for (int i : arr) {\n" +
                "            System.out.print(i + &quot; &quot;);\n" +
                "        }\n" +
                "    }\n" +
                " \n" +
                "    private void sort(int[] arr, int low, int high) {\n" +
                "        if (low &lt; high) {\n" +
                "            int pi = partition(arr, low, high);\n" +
                " \n" +
                "            sort(arr, low, pi - 1);\n" +
                "            sort(arr, pi + 1, high);\n" +
                "        }\n" +
                "    }\n" +
                " \n" +
                "    private int partition(int[] arr, int low, int high) {\n" +
                "        int pivot = arr[high];\n" +
                "        int i = (low - 1);\n" +
                " \n" +
                "        for (int j = low; j &lt; high; j++) {\n" +
                "            if (arr[j] &lt;= pivot) {\n" +
                "                i++;\n" +
                " \n" +
                "                int temp = arr[i];\n" +
                "                arr[i] = arr[j];\n" +
                "                arr[j] = temp;\n" +
                "            }\n" +
                "        }\n" +
                " \n" +
                "        int temp = arr[i + 1];\n" +
                "        arr[i + 1] = arr[high];\n" +
                "        arr[high] = temp;\n" +
                " \n" +
                "        return i + 1;\n" +
                "    }\n" +
                "}\n" +
                "</code></pre>\n" +
                "<p style=\"margin-top: 5px; margin-bottom: 5px;\">This code will sort an array of integers using the QuickSort algorithm. The <code style=\"color: rgb(163, 21, 21)\">sort</code> method recursively calls itself to divide the array into sub-arrays and the <code style=\"color: rgb(163, 21, 21)\">partition</code> method selects a pivot element and rearranges the array such that all elements smaller than the pivot are on the left and all elements greater than the pivot are on the right.</p>\n" +
                "<p style=\"margin-top: 5px; margin-bottom: 5px;\">Note that there are other fast sorting algorithms available in Java, such as MergeSort and Arrays.sort() using a modified version of QuickSort called Dual-Pivot QuickSort.</p>\n";

        Assert.assertEquals(expected, result);
    }
}
