package org.roxycode.jsmashy.service;

import jakarta.inject.Singleton;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.roxycode.jsmashy.generated.Java20Lexer;
import org.roxycode.jsmashy.generated.Java20Parser;
import org.roxycode.jsmashy.visitor.JavaSkeletonListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class FileProcessingService {
    private static final Logger LOG = LoggerFactory.getLogger(FileProcessingService.class);

    public String processDirectory(Path root, List<String> excludes, boolean raw) {
        StringBuilder xml = new StringBuilder("<codebase>\n");
        
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Path> files = findFiles(root, excludes);
            
            List<FileResult> results = files.stream()
                .map(path -> executor.submit(() -> processFile(root, path, raw)))
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        LOG.error("Error processing file", e);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

            for (FileResult res : results) {
                xml.append("  <file path=\"").append(res.relativePath).append("\">\n");
                xml.append("    <![CDATA[").append(res.content.replace("]]>", "]]>]]&gt;<![CDATA[")).append("]]>\n");
                xml.append("  </file>\n");
            }
        } catch (Exception e) {
            LOG.error("Error in parallel processing", e);
        }

        xml.append("</codebase>");
        return xml.toString();
    }

    private List<Path> findFiles(Path root, List<String> excludes) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> !isExcluded(p, excludes))
                .collect(Collectors.toList());
        }
    }

    private boolean isExcluded(Path path, List<String> excludes) {
        String pathStr = path.toString();
        return excludes.stream().anyMatch(pathStr::contains);
    }

    private FileResult processFile(Path root, Path file, boolean raw) {
        try {
            String content = Files.readString(file);
            String relativePath = root.relativize(file).toString();
            
            if (raw || !file.toString().endsWith(".java")) {
                return new FileResult(relativePath, content);
            }

            // Semantic compression for Java
            CharStream input = CharStreams.fromString(content);
            Java20Lexer lexer = new Java20Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Java20Parser parser = new Java20Parser(tokens);
            
            Java20Parser.CompilationUnitContext tree = parser.compilationUnit();
            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
            JavaSkeletonListener listener = new JavaSkeletonListener(rewriter);
            
            ParseTreeWalker.DEFAULT.walk(listener, tree);
            
            return new FileResult(relativePath, rewriter.getText());
        } catch (Exception e) {
            LOG.error("Failed to process " + file, e);
            return null;
        }
    }

    private static record FileResult(String relativePath, String content) {}
}
