package org.roxycode.jsmashy.service;

import jakarta.inject.Singleton;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.roxycode.jsmashy.generated.Java20Lexer;
import org.roxycode.jsmashy.generated.Java20Parser;
import org.roxycode.jsmashy.visitor.JavaSkeletonListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.jgit.ignore.IgnoreNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

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
                    try { return future.get(); } catch (Exception e) { return null; }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
            for (FileResult res : results) {
                xml.append("  <file path=\"").append(res.relativePath).append("\">\n");
                xml.append("    <![CDATA[").append(res.content.replace("]]>", "]]>]]&gt;<![CDATA[")).append("]]>\n");
                xml.append("  </file>\n");
            }
        } catch (Exception e) { LOG.error("Error", e); }
        xml.append("</codebase>");
        return xml.toString();
    }

                                                private List<Path> findFiles(Path root, List<String> excludes) throws IOException {
        List<Path> files = new ArrayList<>();
        Map<Path, List<String>> gitIgnoreMap = new HashMap<>();
        
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String name = dir.getFileName().toString();
                if (name.equals(".git") || isExcluded(dir, excludes)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals(".gitignore")) {
                    gitIgnoreMap.put(file.getParent(), Files.readAllLines(file).stream()
                        .map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                String name = dir.getFileName().toString();
                if (name.equals(".git") || isExcluded(dir, excludes)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.equals(".gitignore") || fileName.equals(".git")) {
                    return FileVisitResult.CONTINUE;
                }
                
                // No need to check isExcluded(file, excludes) here as parents are already checked in preVisitDirectory
                // and files are checked individually if they match the patterns.
                // However, for consistency with isExcluded logic:
                if (isExcluded(file, excludes)) {
                    return FileVisitResult.CONTINUE;
                }

                boolean ignored = false;
                Path current = file;
                while (current != null && current.startsWith(root)) {
                    Path dir = Files.isDirectory(current) ? current : current.getParent();
                    List<String> patterns = gitIgnoreMap.get(dir);
                    if (patterns != null) {
                        String rel = dir.relativize(file).toString().replace("\\\\", "/");
                        for (String p : patterns) {
                            if (rel.equals(p) || (p.endsWith("/") && rel.startsWith(p)) || rel.startsWith(p + "/") || rel.endsWith("/" + p)) {
                                ignored = true; break;
                            }
                        }
                    }
                    if (ignored || dir.equals(root)) break;
                    current = dir.getParent();
                }
                if (!ignored) files.add(file);
                
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

                                            

    private boolean isExcluded(Path path, List<String> excludes) {
        String pathStr = path.toString();
        return excludes.stream().anyMatch(pathStr::contains);
    }

    private FileResult processFile(Path root, Path file, boolean raw) {
        try {
            String content = Files.readString(file);
            String relativePath = root.relativize(file).toString().replace("\\", "/");
            if (raw || !file.toString().endsWith(".java")) {
                return new FileResult(relativePath, content);
            }
            CharStream input = CharStreams.fromString(content);
            Java20Lexer lexer = new Java20Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            Java20Parser parser = new Java20Parser(tokens);
            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
            JavaSkeletonListener listener = new JavaSkeletonListener(rewriter);
            ParseTreeWalker.DEFAULT.walk(listener, parser.compilationUnit());
            return new FileResult(relativePath, rewriter.getText());
        } catch (Exception e) { return null; }
    }

    private static record FileResult(String relativePath, String content) {}
}