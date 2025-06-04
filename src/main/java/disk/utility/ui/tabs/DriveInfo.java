// src/main/java/disk/utility/ui/tabs/DriveInfo.java
package disk.utility.ui.tabs;

import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DriveInfo {

    private final SimpleStringProperty letter = new SimpleStringProperty();
    private final SimpleStringProperty total = new SimpleStringProperty();
    private final SimpleStringProperty free = new SimpleStringProperty();

    private final long totalBytes;
    private final long freeBytes;

    public DriveInfo(String letter, long totalBytes, long freeBytes) {
        this.letter.set(letter);
        this.totalBytes = totalBytes;
        this.freeBytes = freeBytes;
        this.total.set(human(totalBytes));
        this.free.set(human(freeBytes));
    }

    // -------- гетери для JavaFX TableView --------
    public SimpleStringProperty letterProperty() {
        return letter;
    }

    public SimpleStringProperty totalProperty() {
        return total;
    }

    public SimpleStringProperty freeProperty() {
        return free;
    }

    // -------- гетери для логіки --------
    public long totalBytes() {
        return totalBytes;
    }

    public long freeBytes() {
        return freeBytes;
    }

    public long usedBytes() {
        return totalBytes - freeBytes;
    }

    /* Convert bytes → KB/MB/GB string */
    private static String human(long b) {
        String[] u = { "B", "KB", "MB", "GB", "TB" };
        int i = 0;
        double v = b;
        while (v >= 1024 && i < u.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format("%.1f %s", v, u[i]);
    }

    /** Зчитати усі реальні диски */
    public static List<DriveInfo> fetch() {
        List<DriveInfo> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            long t = root.getTotalSpace();
            long f = root.getFreeSpace();
            if (t == 0)
                continue; // CD/DVD без диска
            list.add(new DriveInfo(root.getPath(), t, f));
        }
        return list;
    }
}
