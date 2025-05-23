package disk.utility.ui.tabs;

import disk.utility.AppContext;
import disk.utility.duplicate.DuplicateFinderService;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DuplicateFinderTab extends Tab {

    private final CheckBox cbName;
    private final CheckBox cbSize;
    private final CheckBox cbDate;
    private final CheckBox cbHash;

    private final CheckBox igZero;
    private final Spinner<Double> minSize;
    private final Spinner<Double> maxSize;

    private final ListView<Path> includeList = new ListView<>();
    private final ListView<Path> excludeList = new ListView<>();

    private final ProgressBar bar = new ProgressBar(0);
    private final Label perc = new Label("0 %");
    private final ProgressIndicator spinner = new ProgressIndicator();

    private final Label status = new Label();

    private final VBox resultsBox = new VBox(8);
    private final ScrollPane scroll = new ScrollPane(resultsBox);

    private final Button deleteBtn = new Button();
    private final BooleanProperty somethingSelected = new SimpleBooleanProperty(false);

    private Task<?> current;
    private VBox leftPane;
    private final ResourceBundle bundle;

    public DuplicateFinderTab(ResourceBundle bundle) {
        super(bundle.getString("tab.duplicates"));
        this.bundle = bundle;

        cbName = new CheckBox(bundle.getString("label.name"));
        cbSize = new CheckBox(bundle.getString("label.size"));
        cbDate = new CheckBox(bundle.getString("label.date"));
        cbHash = new CheckBox(bundle.getString("label.content"));

        igZero = new CheckBox(bundle.getString("label.ignore_zero"));
        minSize = new Spinner<>();
        maxSize = new Spinner<>();
        minSize.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Long.MAX_VALUE, 0));
        maxSize.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, Long.MAX_VALUE, 1024));

        cbName.setSelected(true);
        cbSize.setSelected(true);
        cbHash.setSelected(true);
        igZero.setSelected(true);

        FlowPane critPane = new FlowPane(10, 5, cbName, cbSize, cbDate, cbHash);
        VBox ignPane = new VBox(6, igZero,
                new HBox(4, new Label(bundle.getString("label.min_mb")), minSize),
                new HBox(4, new Label(bundle.getString("label.max_mb")), maxSize));

        Button addInc = new Button(bundle.getString("button.add"));
        addInc.setOnAction(e -> pickDir(includeList));
        Button addExc = new Button(bundle.getString("button.add"));
        addExc.setOnAction(e -> pickDir(excludeList));
        includeList.setPrefHeight(80);
        excludeList.setPrefHeight(80);
        VBox incBox = new VBox(4, new Label(bundle.getString("label.include")), includeList, addInc);
        VBox excBox = new VBox(4, new Label(bundle.getString("label.exclude")), excludeList, addExc);
        SplitPane dirSplit = new SplitPane(incBox, excBox);
        dirSplit.setDividerPositions(0.5);

        Button search = new Button(bundle.getString("button.search"));
        search.setOnAction(e -> runSearch());

        spinner.setVisible(false);
        spinner.setPrefSize(22, 22);
        deleteBtn.setText(bundle.getString("button.delete_selected"));
        deleteBtn.setVisible(false);
        deleteBtn.setDisable(true);
        deleteBtn.setOnAction(e -> deleteSelected());

        leftPane = new VBox(10,
                titled(bundle.getString("label.criteria"), critPane),
                titled(bundle.getString("label.ignore"), ignPane),
                titled(bundle.getString("label.folders"), dirSplit),
                search,
                bar, perc, spinner, status);
        leftPane.setPadding(new Insets(10));
        VBox.setVgrow(dirSplit, Priority.ALWAYS);

        scroll.setFitToWidth(true);
        resultsBox.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setLeft(leftPane);
        root.setCenter(scroll);
        root.setBottom(new StackPane(deleteBtn) {
            {
                setPadding(new Insets(8));
                deleteBtn.visibleProperty().bind(somethingSelected);
            }
        });
        deleteBtn.maxWidthProperty().bind(root.widthProperty().subtract(30));

        setContent(root);
    }

    private void runSearch() {
        if (includeList.getItems().isEmpty()) {
            status.setText(bundle.getString("status.select_directory"));
            return;
        }

        clearResults();
        resetIndicators();
        spinner.setVisible(true);
        blockLeft(true);
        status.setText(bundle.getString("status.scanning"));

        DuplicateFinderService.Options opt = new DuplicateFinderService.Options(
                cbName.isSelected(), cbSize.isSelected(), cbDate.isSelected(), cbHash.isSelected(),
                igZero.isSelected(), false, false, false,
                minSize.getValue().longValue() * 1024L * 1024,
                maxSize.getValue().longValue() * 1024L * 1024,
                new ArrayList<>(includeList.getItems()),
                new ArrayList<>(excludeList.getItems()));

        current = new Task<>() {
            @Override
            protected List<List<Path>> call() {
                return DuplicateFinderService.findDuplicates(opt,
                        (done, total) -> updateProgress((double) done, (double) total));
            }
        };

        current.setOnSucceeded(e -> {
            List<List<Path>> result = ((Task<List<List<Path>>>) e.getSource()).getValue();
            if (result == null || result.isEmpty())
                status.setText(bundle.getString("status.no_duplicates"));
            else {
                buildResults(result);
                status.setText(bundle.getString("status.groups") + result.size());
            }
            finish();
        });

        current.setOnFailed(e -> {
            status.setText(bundle.getString("status.error") + current.getException().getMessage());
            finish();
        });

        bar.progressProperty().bind(current.progressProperty());
        perc.textProperty().bind(current.progressProperty().multiply(100).asString("%.0f %%"));

        AppContext.EXECUTOR.submit(current);
    }

    private void buildResults(List<List<Path>> groups) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int gid = 1;
        for (List<Path> g : groups) {
            long total = g.stream().mapToLong(this::sizeSafe).sum();
            VBox filesBox = new VBox(4);
            filesBox.setPadding(new Insets(4));

            for (Path p : g) {
                long sz = sizeSafe(p);
                CheckBox cb = new CheckBox(p.toString() + "  (" + human(sz) + ")");
                cb.setUserData(p);
                cb.selectedProperty().addListener((obs, o, n) -> updateSelectionState());
                filesBox.getChildren().add(cb);
            }
            TitledPane pane = new TitledPane("#" + gid + " - " + g.size() + " files (" + human(total) + ")", filesBox);
            pane.setExpanded(true);
            pane.setPadding(new Insets(2));
            resultsBox.getChildren().add(pane);
            gid++;
        }
    }

    private void deleteSelected() {
        List<CheckBox> sel = resultsBox.getChildren().stream()
                .filter(n -> n instanceof TitledPane)
                .map(TitledPane.class::cast)
                .flatMap(tp -> ((VBox) tp.getContent()).getChildren().stream())
                .filter(n -> n instanceof CheckBox && ((CheckBox) n).isSelected())
                .map(CheckBox.class::cast)
                .collect(Collectors.toList());
        if (sel.isEmpty())
            return;

        String message = bundle.getString("alert.delete_confirm") + sel.size()
                + bundle.getString("alert.delete_confirm_files");
        if (new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO)
                .showAndWait().orElse(ButtonType.NO) != ButtonType.YES)
            return;

        int ok = 0, fail = 0;
        for (CheckBox cb : sel) {
            Path p = (Path) cb.getUserData();
            try {
                if (Files.deleteIfExists(p))
                    ok++;
                else
                    fail++;
            } catch (IOException ex) {
                fail++;
            }
        }
        status.setText(bundle.getString("status.deleted") + ok
                + (fail > 0 ? "  " + bundle.getString("status.failed") + fail : ""));
        refreshAfterDeletion();
        updateSelectionState();
    }

    private void refreshAfterDeletion() {
        resultsBox.getChildren().removeIf(tp -> {
            if (!(tp instanceof TitledPane))
                return false;
            VBox box = (VBox) ((TitledPane) tp).getContent();
            box.getChildren().removeIf(n -> n instanceof CheckBox && ((CheckBox) n).isSelected());
            return box.getChildren().isEmpty();
        });
    }

    private void finish() {
        resetIndicators();
        spinner.setVisible(false);
        blockLeft(false);
        current = null;
    }

    private void resetIndicators() {
        if (bar.progressProperty().isBound())
            bar.progressProperty().unbind();
        if (perc.textProperty().isBound())
            perc.textProperty().unbind();
        bar.setProgress(0);
        perc.setText("0 %");
    }

    private void blockLeft(boolean d) {
        leftPane.getChildren().forEach(n -> {
            if (!(n instanceof ProgressBar) && !(n instanceof Label) && n != spinner)
                n.setDisable(d);
        });
    }

    private void updateSelectionState() {
        boolean any = resultsBox.getChildren().stream()
                .filter(tp -> tp instanceof TitledPane)
                .map(TitledPane.class::cast)
                .flatMap(tp -> {
                    Node content = tp.getContent();
                    if (content instanceof VBox) {
                        return ((VBox) content).getChildrenUnmodifiable().stream();
                    }
                    return Stream.<Node>empty();
                })
                .anyMatch(n -> n instanceof CheckBox && ((CheckBox) n).isSelected());
        somethingSelected.set(any);
        deleteBtn.setDisable(!any);
    }

    private void clearResults() {
        resultsBox.getChildren().clear();
        somethingSelected.set(false);
    }

    private void pickDir(ListView<Path> list) {
        File dir = new DirectoryChooser().showDialog(getTabPane().getScene().getWindow());
        if (dir != null)
            list.getItems().add(dir.toPath());
    }

    private long sizeSafe(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }

    private static TitledPane titled(String t, Node n) {
        TitledPane p = new TitledPane(t, n);
        p.setCollapsible(false);
        return p;
    }

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
}
