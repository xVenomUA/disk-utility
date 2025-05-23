package disk.utility.duplicate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;

public class DuplicateFinderService {

    /* ---------------- options ---------------- */
    public record Options(
            boolean matchName,
            boolean matchSize,
            boolean matchDate,
            boolean matchContent,
            boolean ignoreZero,
            boolean ignoreSys,
            boolean ignoreRO,
            boolean ignoreHidden,
            long minBytes,
            long maxBytes,
            List<Path> includes,
            List<Path> excludes) {
    }

    /* ---------------- main API ---------------- */
    public static List<List<Path>> findDuplicates(Options opt, BiConsumer<Long, Long> progress) {
        List<Path> files = new ArrayList<>();

        /* 1. collect all candidate files */
        for (Path root : opt.includes) {
            if (!Files.exists(root))
                continue;
            try {
                Files.walkFileTree(root, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path p, BasicFileAttributes a) {
                        if (shouldSkip(p, a, opt))
                            return FileVisitResult.CONTINUE;
                        files.add(p);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path f, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ignored) {
            }
        }

        /* 2. group */
        Map<Key, List<Path>> map = new HashMap<>();
        long done = 0, total = files.size();

        for (Path p : files) {
            try {
                BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class);
                Key k = new Key(
                        opt.matchName ? p.getFileName().toString().toLowerCase() : "",
                        opt.matchSize ? a.size() : -1,
                        opt.matchDate ? a.lastModifiedTime().toMillis() : -1,
                        opt.matchContent ? fileHash(p) : "");
                map.computeIfAbsent(k, __ -> new ArrayList<>()).add(p);
            } catch (IOException ignored) {
            }
            progress.accept(++done, total);
        }

        /* 3. return only groups with size > 1, sorted descending */
        List<List<Path>> result = new ArrayList<>(map.values());
        result.removeIf(l -> l.size() < 2);
        result.sort((a, b) -> Integer.compare(b.size(), a.size()));

        progress.accept(total, total);
        return result;
    }

    /* ---------------- helpers ---------------- */

    private static boolean shouldSkip(Path p, BasicFileAttributes a, Options o) {
        for (Path ex : o.excludes)
            if (p.startsWith(ex))
                return true;
        try {
            if (o.ignoreHidden && Files.isHidden(p))
                return true;
            if (o.ignoreSys && p.toString().startsWith("$"))
                return true;
            if (o.ignoreRO && !Files.isWritable(p))
                return true;
        } catch (IOException ignored) {
        }
        long sz = a.size();
        if (o.ignoreZero && sz == 0)
            return true;
        return (sz < o.minBytes || sz > o.maxBytes);
    }

    /* SHA‑256 без сторонніх бібліотек */
    private static String fileHash(Path p) {
        try (InputStream in = Files.newInputStream(p);
                DigestInputStream dis = new DigestInputStream(in, MessageDigest.getInstance("SHA-256"))) {

            byte[] buf = new byte[8192];
            while (dis.read(buf) != -1) {
                /* digest оновлюється */ }

            byte[] hash = dis.getMessageDigest().digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash)
                sb.append(String.format("%02x", b));
            return sb.toString();

        } catch (IOException | NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString(); // fallback
        }
    }

    private record Key(String n, long s, long d, String h) {
    }
}
