package org.roxycode.jsmashy;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.roxycode.jsmashy.service.FileProcessingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class SemanticCompressionTest {

    @Inject
    FileProcessingService fileProcessingService;

    @Test
    void testJavaCompression() throws IOException {
        Path tempDir = Files.createTempDirectory("jsmashy-test-java");
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, "public class Test { public void method() { System.out.println(1); } }");

        String xml = fileProcessingService.processDirectory(tempDir, List.of(), false);
        assertTrue(xml.contains("public class Test"), "Java class should be present");
        assertFalse(xml.contains("System.out.println"), "Java body should be stripped");
    }

    @Test
    void testPythonAndRawCompression() throws IOException {
        Path tempDir = Files.createTempDirectory("jsmashy-test-py-raw");
        
        // Python file
        Path pyFile = tempDir.resolve("test.py");
        Files.writeString(pyFile, "class A:\n    def f(self):\n        print(1)\n\ndef top():\n    pass");

        // Plain text file (should be raw)
        Path txtFile = tempDir.resolve("info.txt");
        Files.writeString(txtFile, "Hello World");

        // File with CDATA end sequence (should be escaped)
        Path cdataFile = tempDir.resolve("cdata.txt");
        Files.writeString(cdataFile, "]]>");

        String xml = fileProcessingService.processDirectory(tempDir, List.of(), false);
        System.out.println("COMPLEX DEBUG XML:\n" + xml);

        assertTrue(xml.contains("class A:"), "Python class should be present");
        assertTrue(xml.contains("def f(self): ..."), "Python method should be stripped");
        assertTrue(xml.contains("def top(): ..."), "Python top level func should be stripped");
        
        assertTrue(xml.contains("<file path=\"info.txt\">"), "Text file should be present");
        assertTrue(xml.contains("Hello World"), "Text file should be raw");
        
        assertTrue(xml.contains("<file path=\"cdata.txt\">"), "CDATA file should be present");
        assertTrue(xml.contains("]]>]]&gt;<![CDATA["), "CDATA end sequence should be escaped");
    }
}