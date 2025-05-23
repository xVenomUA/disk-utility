package disk.utility.ui.tabs;

import disk.utility.AppContext;
import disk.utility.ui.components.DirectoryPicker;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;
import java.util.ResourceBundle;

public class DiskWiperTab extends Tab {
    public DiskWiperTab(ResourceBundle bundle) {
        super(bundle.getString("tab.wiper"));

        DirectoryPicker picker = new DirectoryPicker(bundle.getString("tab.wiperText"));
        Spinner<Integer> passes = new Spinner<>(1, 7, 3);
        Button wipe = new Button(bundle.getString("button.wipe"));
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(300);
        bar.setVisible(false);

        wipe.setOnAction(e -> {
            String pathStr = picker.getPath();
            if (pathStr.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, bundle.getString("alert.select_path")).show();
                return;
            }
            Path path = Paths.get(pathStr);
            int passCount = passes.getValue();

            bar.setProgress(0);
            bar.setVisible(true);

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    if (Files.isDirectory(path)) {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                wipeFile(file, passCount);
                                updateProgress(1, 1);
                                return FileVisitResult.CONTINUE;
                            }
                            @Override
                            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                                Files.delete(dir);
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } else {
                        wipeFile(path, passCount);
                    }
                    return null;
                }
            };
            bar.progressProperty().bind(task.progressProperty());
            task.setOnSucceeded(ev -> {
                bar.setVisible(false);
                new Alert(Alert.AlertType.INFORMATION, bundle.getString("alert.wipe_completed")).show();
            });
            task.setOnFailed(ev -> {
                bar.setVisible(false);
                new Alert(Alert.AlertType.ERROR, bundle.getString("alert.wipe_failed") + task.getException().getMessage()).show();
            });
            AppContext.EXECUTOR.submit(task);
        });

        setContent(new VBox(10, picker,
                new HBox(8, new Label(bundle.getString("label.passes")), passes), wipe, bar));
    }

    private void wipeFile(Path file, int passes) throws IOException {
        if (!Files.isRegularFile(file)) return;

        long size = Files.size(file);
        byte[] buffer = new byte[65536];
        Random random = new Random();

        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
            for (int p = 0; p < passes; p++) {
                raf.seek(0);
                long remaining = size;
                while (remaining > 0) {
                    int chunk = (int) Math.min(buffer.length, remaining);
                    random.nextBytes(buffer);
                    raf.write(buffer, 0, chunk);
                    remaining -= chunk;
                }
                raf.getChannel().force(true);
            }
        }
        Files.delete(file);
    }
}