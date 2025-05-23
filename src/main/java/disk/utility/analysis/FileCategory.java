// src/main/java/disk/utility/analysis/FileCategory.java
package disk.utility.analysis;

import java.nio.file.Path;
import java.util.Set;

public enum FileCategory {
    IMAGES("Images", Set.of("jpg", "jpeg", "png", "gif", "bmp", "tiff")),
    VIDEOS("Videos", Set.of("mp4", "avi", "mkv", "mov", "wmv", "flv")),
    MUSIC("Music", Set.of("mp3", "wav", "aac", "flac", "ogg")),
    DOCS("Documents", Set.of("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")),
    COMPRESSED("Compressed", Set.of("zip", "rar", "7z", "gz", "tar")),
    EMAIL("Email", Set.of("eml", "pst", "ost", "msg", "mbox")),
    OTHER("Other", Set.of());

    public final String label;
    private final Set<String> exts;

    FileCategory(String label, Set<String> exts) {
        this.label = label;
        this.exts = exts;
    }

    public boolean matches(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot == -1)
            return this == OTHER;
        String ext = name.substring(dot + 1);
        if (exts.contains(ext))
            return true;
        return this == OTHER;
    }

    public static FileCategory of(Path p) {
        for (FileCategory c : values())
            if (c != OTHER && c.matches(p))
                return c;
        return OTHER;
    }
}
