package disk.utility.ui.tabs;

import disk.utility.AppContext;
import javafx.application.Platform;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class DiskOverviewTab extends Tab {

    private final ObservableList<DriveInfo> tableData = FXCollections.observableArrayList();

    private final TableView<DriveInfo> table;
    private final TilePane chartsPane = new TilePane(10, 10);
    private final ResourceBundle bundle ;

    public DiskOverviewTab(ResourceBundle bundle) {
        super(bundle.getString("tab.overview"));
        this.bundle = bundle;
        this.table = buildTable();

        chartsPane.setPadding(new Insets(10));
        chartsPane.setPrefColumns(2);

        ScrollPane scroll = new ScrollPane(chartsPane);
        scroll.setFitToWidth(true);

        Button refreshBtn = new Button(bundle.getString("button.refresh"));
        refreshBtn.setOnAction(e -> refreshOnce());

        HBox topBox = new HBox(10,
                new Label(bundle.getString("label.drives")),
                refreshBtn);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10,
                topBox,
                table,
                scroll);
        table.setPrefHeight(100);
        setContent(content);

        refreshOnce();
        startAutoRefresh();
    }

    private TableView<DriveInfo> buildTable() {
        TableView<DriveInfo> tv = new TableView<>(tableData);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DriveInfo, String> cD = new TableColumn<>(bundle.getString("column.drive"));
        cD.setCellValueFactory(c -> c.getValue().letterProperty());

        TableColumn<DriveInfo, String> cT = new TableColumn<>(bundle.getString("column.total"));
        cT.setCellValueFactory(c -> c.getValue().totalProperty());

        TableColumn<DriveInfo, String> cF = new TableColumn<>(bundle.getString("column.free"));
        cF.setCellValueFactory(c -> c.getValue().freeProperty());

        tv.getColumns().addAll(cD, cT, cF);
        return tv;
    }

    private void refreshOnce() {
        List<DriveInfo> drives = DriveInfo.fetch();
        tableData.setAll(drives);

        chartsPane.getChildren().clear();
        for (DriveInfo d : drives) {
            chartsPane.getChildren().add(buildChartFor(d));
        }
    }

    private PieChart buildChartFor(DriveInfo d) {
        long used = d.usedBytes();
        long free = d.freeBytes();

        PieChart.Data usedSlice = new PieChart.Data(
                bundle.getString("label.used") + "(" + human(used) + ")", used);
        PieChart.Data freeSlice = new PieChart.Data(
                bundle.getString("label.free") + "(" + human(free) + ")", free);

        PieChart pie = new PieChart(
                FXCollections.observableArrayList(usedSlice, freeSlice));

        pie.setTitle(d.letterProperty().get() + " (" + d.totalProperty().get() + ")");
        pie.setLabelsVisible(true);
        pie.setClockwise(true);
        pie.setLegendVisible(false);
        pie.setPrefSize(400, 400);

        return pie;
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

    private void startAutoRefresh() {
        AppContext.EXECUTOR.scheduleAtFixedRate(
                () -> Platform.runLater(this::refreshOnce),
                5, 5, TimeUnit.SECONDS);
    }
}