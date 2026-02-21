package org.roxycode.jsmashy;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.roxycode.jsmashy.service.FileProcessingService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class GitIgnoreTest {

    @Inject
    FileProcessingService fileProcessingService;

    @Test
    void testGitIgnoreRespected(@TempDir Path tempDir) throws IOException {
        // Create a file that should be included
        Files.writeString(tempDir.resolve("include.txt"), "include");
        
        // Create a file that should be ignored
        Files.writeString(tempDir.resolve("ignore.txt"), "ignore");
        
        // Create .gitignore
        Files.writeString(tempDir.resolve(".gitignore"), "ignore.txt\n");
        
        String result = fileProcessingService.processDirectory(tempDir, Collections.emptyList(), true);
        
        assertTrue(result.contains("include.txt"), "Should contain include.txt");
        assertFalse(result.contains("ignore.txt"), "Should NOT contain ignore.txt");
    }

    @Test
    void testNestedGitIgnoreRespected(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        
        Files.writeString(tempDir.resolve("root.txt"), "root");
        Files.writeString(subDir.resolve("nested.txt"), "nested");
        
        // Ignore the subdir contents via root .gitignore
        Files.writeString(tempDir.resolve(".gitignore"), "subdir/\n");
        
        String result = fileProcessingService.processDirectory(tempDir, Collections.emptyList(), true);
        
        assertTrue(result.contains("root.txt"));
        assertFalse(result.contains("subdir/nested.txt"), "Should ignore contents of subdir");
        assertFalse(result.contains("nested.txt"));
    }

    @Test
    void testGitFolderIgnored(@TempDir Path tempDir) throws IOException {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);
        Files.writeString(gitDir.resolve("config"), "some config");
        Files.writeString(tempDir.resolve("code.java"), "public class A {}");

        String result = fileProcessingService.processDirectory(tempDir, List.of("target"), true);

        assertTrue(result.contains("code.java"), "Should contain code.java");
        assertFalse(result.contains(".git/config"), "Should NOT contain .git/config");
        assertFalse(result.contains("some config"), "Should NOT contain content from .git/config");
    }

}