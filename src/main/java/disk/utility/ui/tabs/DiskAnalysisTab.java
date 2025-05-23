// -----------------------------------------------------------------------------
// DiskAnalysisTab.java (виправлений)
// -----------------------------------------------------------------------------
package disk.utility.ui.tabs;

import disk.utility.AppContext;
import disk.utility.analysis.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class DiskAnalysisTab extends Tab {

    public enum SizeUnit {
        BYTES(1),
        KB(1L << 10),
        MB(1L << 20),
        GB(1L << 30),
        TB(1L << 40);

        private final long factor;

        SizeUnit(long factor) {
            this.factor = factor;
        }

        public long getFactor() {
            return factor;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    private final ResourceBundle bundle;

    private final Map<FileCategory, CheckBox> catChecks = Arrays.stream(FileCategory.values())
            .collect(Collectors.toMap(c -> c, c -> new CheckBox(c.label)));
    private final Map<Path, CheckBox> driveChecks = new LinkedHashMap<>();

    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final PieChart pie = new PieChart();
    private final ProgressBar bar = new ProgressBar(0);
    private final Label status = new Label();

    private Task<?> currentTask;

    private final Spinner<Double> minSizeSpinner = new Spinner<>();
    private final Spinner<Double> maxSizeSpinner = new Spinner<>();
    private final DatePicker startDatePicker = new DatePicker();
    private final DatePicker endDatePicker = new DatePicker();
    private final CheckBox useFiltersCheckBox = new CheckBox();
    private final ComboBox<SizeUnit> sizeUnitCombo = new ComboBox<>();

    public DiskAnalysisTab(ResourceBundle bundle) {
        super(bundle.getString("tab.analysis"));
        this.bundle = bundle;

        initSpinners();
        buildUi();
    }

    private void initSpinners() {
        SpinnerValueFactory.DoubleSpinnerValueFactory minFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0,
                Long.MAX_VALUE, 0);
        SpinnerValueFactory.DoubleSpinnerValueFactory maxFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0,
                Long.MAX_VALUE, Long.MAX_VALUE);

        minFactory.setConverter(new DoubleStringConverter());
        maxFactory.setConverter(new DoubleStringConverter());

        minSizeSpinner.setValueFactory(minFactory);
        maxSizeSpinner.setValueFactory(maxFactory);
        minSizeSpinner.setEditable(true);
        maxSizeSpinner.setEditable(true);
    }

    private void buildUi() {
        sizeUnitCombo.getItems().addAll(SizeUnit.values());
        sizeUnitCombo.setValue(SizeUnit.BYTES);

        useFiltersCheckBox.setText(bundle.getString("checkbox.use_filters"));
        useFiltersCheckBox.setSelected(true);

        VBox catBox = new VBox(4);
        catChecks.values().forEach(cb -> {
            cb.setSelected(true);
            catBox.getChildren().add(cb);
        });

        VBox drvBox = new VBox(4);
        DriveInfo.fetch().forEach(d -> {
            CheckBox cb = new CheckBox(d.letterProperty().get());
            cb.setSelected(true);
            driveChecks.put(Path.of(cb.getText()), cb);
            drvBox.getChildren().add(cb);
        });

        VBox filterBox = new VBox(4,
                new HBox(4, new Label(bundle.getString("label.size_unit")), sizeUnitCombo),
                new HBox(4, new Label(bundle.getString("label.min_size")), minSizeSpinner),
                new HBox(4, new Label(bundle.getString("label.max_size")), maxSizeSpinner),
                new HBox(4, new Label(bundle.getString("label.start_date")), startDatePicker),
                new HBox(4, new Label(bundle.getString("label.end_date")), endDatePicker));

        useFiltersCheckBox.selectedProperty().addListener((obs, o, on) -> {
            sizeUnitCombo.setDisable(!on);
            minSizeSpinner.setDisable(!on);
            maxSizeSpinner.setDisable(!on);
            startDatePicker.setDisable(!on);
            endDatePicker.setDisable(!on);
        });

        Button analyze = new Button(bundle.getString("button.analyze"));
        analyze.setOnAction(e -> runAnalysis());

        TableView<Row> table = buildTable();
        table.setPrefHeight(180);

        pie.setLegendVisible(false);
        pie.setLabelsVisible(true);
        pie.setMinSize(300, 300);

        VBox resultBox = new VBox(table, pie);
        VBox.setVgrow(pie, Priority.ALWAYS);

        VBox left = new VBox(10,
                titled(bundle.getString("label.categories"), catBox),
                titled(bundle.getString("label.drives"), drvBox),
                titled(bundle.getString("label.filters"), filterBox),
                useFiltersCheckBox, analyze, bar, status);
        left.setPadding(new Insets(10));

        SplitPane split = new SplitPane(left, resultBox);
        split.setDividerPositions(0.3);
        setContent(split);
    }

    private TableView<Row> buildTable() {
        TableView<Row> tv = new TableView<>(rows);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Row, String> c1 = new TableColumn<>(bundle.getString("table.category"));
        c1.setCellValueFactory(r -> r.getValue().catProperty());

        TableColumn<Row, String> c2 = new TableColumn<>(bundle.getString("table.size"));
        c2.setCellValueFactory(r -> r.getValue().sizeProperty());

        tv.getColumns().addAll(c1, c2);
        return tv;
    }

    private void runAnalysis() {
        bar.progressProperty().unbind();
        bar.setProgress(0);
        status.setText(bundle.getString("status.please_wait"));

        if (currentTask != null && currentTask.isRunning())
            currentTask.cancel();

        Set<FileCategory> selectedCats = catChecks.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Path> roots = driveChecks.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .toList();

        if (roots.isEmpty()) {
            status.setText(bundle.getString("status.select_drive"));
            return;
        }
        if (selectedCats.isEmpty()) {
            status.setText(bundle.getString("status.select_category"));
            return;
        }

        catChecks.values().forEach(cb -> cb.setDisable(true));
        driveChecks.values().forEach(cb -> cb.setDisable(true));

        long minSizeBytes = 0;
        long maxSizeBytes = Long.MAX_VALUE;
        long minModified = 0, maxModified = Long.MAX_VALUE;

        if (useFiltersCheckBox.isSelected()) {
            SizeUnit unit = sizeUnitCombo.getValue();
            double minVal = minSizeSpinner.getValue();
            double maxVal = maxSizeSpinner.getValue();

            minSizeBytes = (long) (minVal * unit.getFactor());
            maxSizeBytes = (long) (maxVal * unit.getFactor());

            if (minSizeBytes < 0 || maxSizeBytes < 0) {
                showError(bundle.getString("error.size_too_large.title"),
                        bundle.getString("error.size_too_large.header"),
                        bundle.getString("error.size_too_large.content"));
                reEnable();
                return;
            }

            LocalDate s = startDatePicker.getValue();
            LocalDate e = endDatePicker.getValue();
            minModified = s != null
                    ? s.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : 0;
            maxModified = e != null
                    ? e.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : Long.MAX_VALUE;
        }

        long fMinSize = minSizeBytes;
        long fMaxSize = maxSizeBytes;
        long fMinMod = minModified;
        long fMaxMod = maxModified;

        currentTask = new Task<>() {
            @Override
            protected Map<FileCategory, Long> call() {
                return DiskAnalyzer.analyze(
                        roots,
                        fMinSize,
                        fMaxSize,
                        fMinMod,
                        fMaxMod,
                        this::updateProgress);
            }
        };

        currentTask.setOnSucceeded(ev -> {
            @SuppressWarnings("unchecked")
            Map<FileCategory, Long> result = ((Task<Map<FileCategory, Long>>) ev.getSource()).getValue();
            populate(selectedCats, result);
            reEnable();
            status.setText(bundle.getString("status.done"));
        });

        currentTask.setOnFailed(ev -> {
            status.setText(bundle.getString("status.error") + ev.getSource().getException().getMessage());
            ev.getSource().getException().printStackTrace();
            reEnable();
        });

        bar.progressProperty().bind(currentTask.progressProperty());
        AppContext.EXECUTOR.submit(currentTask);
    }

    private void reEnable() {
        bar.progressProperty().unbind();
        bar.setProgress(0);
        catChecks.values().forEach(cb -> cb.setDisable(false));
        driveChecks.values().forEach(cb -> cb.setDisable(false));
    }

    private void showError(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    private void populate(Set<FileCategory> selected, Map<FileCategory, Long> sizes) {
        rows.clear();
        ObservableList<PieChart.Data> slice = FXCollections.observableArrayList();

        selected.forEach(cat -> {
            long sz = sizes.getOrDefault(cat, 0L);
            rows.add(new Row(cat.label, human(sz)));
            if (sz > 0)
                slice.add(new PieChart.Data(cat.label + " (" + human(sz) + ")", sz));
        });
        pie.setData(slice);

        Platform.runLater(() -> {
            pie.setMinSize(400, 400);
            pie.applyCss();
            pie.layout();
        });
    }

    private record Row(SimpleStringProperty cat, SimpleStringProperty size) {
        Row(String c, String s) {
            this(new SimpleStringProperty(c), new SimpleStringProperty(s));
        }

        SimpleStringProperty catProperty() {
            return cat;
        }

        SimpleStringProperty sizeProperty() {
            return size;
        }
    }

    private static TitledPane titled(String h, VBox box) {
        TitledPane t = new TitledPane(h, box);
        t.setCollapsible(false);
        return t;
    }

    private static String human(long b) {
        if (b == 0)
            return "0 B";
        String[] u = { "B", "KB", "MB", "GB", "TB" };
        int i = 0;
        double v = b;
        while (v >= 1024 && i < u.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format("%.1f %s", v, u[i]);
    }

    private static class LongStringConverter extends StringConverter<Long> {
        @Override
        public String toString(Long value) {
            return value == null ? "0" : value.toString();
        }

        @Override
        public Long fromString(String s) {
            if (s == null || s.isBlank())
                return 0L;
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
    }
}
