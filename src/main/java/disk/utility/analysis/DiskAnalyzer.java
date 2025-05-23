package disk.utility.analysis;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class DiskAnalyzer {

    public static Map<FileCategory, Long> analyze(List<Path> roots,
            long minSize,
            long maxSize,
            long minModified,
            long maxModified,
            BiConsumer<Long, Long> progress) {

        long totalBytes = roots.stream()
                .mapToLong(p -> silent(() -> Files.getFileStore(p).getTotalSpace()))
                .sum();
        final long grandTotal = totalBytes == 0 ? 1 : totalBytes;

        EnumMap<FileCategory, Long> sizes = new EnumMap<>(FileCategory.class);
        for (FileCategory c : FileCategory.values())
            sizes.put(c, 0L);

        long[] done = { 0 };
        for (Path root : roots) {
            walkSafe(root, (file, attrs) -> {
                if (!attrs.isRegularFile())
                    return;

                long sz = attrs.size();
                long lastModified = attrs.lastModifiedTime().toMillis();

                if (sz < minSize || sz > maxSize
                        || lastModified < minModified
                        || lastModified > maxModified)
                    return;

                FileCategory cat = FileCategory.of(file);
                sizes.merge(cat, sz, Long::sum);

                done[0] += sz;
                progress.accept(done[0], grandTotal);
            });
        }
        return sizes;
    }

    private interface FileVisitor {
        void visit(Path file, BasicFileAttributes attrs) throws IOException;
    }

    private static void walkSafe(Path root, FileVisitor v) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) {
                    try {
                        v.visit(file, attrs);
                    } catch (IOException ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static long silent(IOSupplier s) {
        try {
            return s.get();
        } catch (IOException e) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface IOSupplier {
        long get() throws IOException;
    }
}