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
        Path tempDir = Files.createTempDirectory("jsmashy-test");
        Path javaFile = tempDir.resolve("Test.java");
        Files.writeString(javaFile, """
            package com.example;
            /** Javadoc */
            public class Test {
                // Comment
                public void method() {
                    System.out.println("Hello");
                }
                
                public Test() {
                    this.init();
                }
            }
            """);

        String xml = fileProcessingService.processDirectory(tempDir, List.of(), false); System.out.println("DEBUG XML: " + xml);
        
        assertTrue(xml.contains("public class Test"), "Should contain class decl");
        assertTrue(xml.contains("public void method()") && xml.contains(";"), "Should contain method decl");
        assertTrue(xml.contains("public Test()") && xml.contains(";"), "Should contain constructor decl");
        assertFalse(xml.contains("System.out.println"), "Should NOT contain method body");
        assertFalse(xml.contains("Javadoc"), "Should NOT contain comments");
        assertFalse(xml.contains("Comment"), "Should NOT contain comments");
    }
}
