package disk.utility.ui;

import disk.utility.MainApp;
import disk.utility.ui.tabs.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import java.util.ResourceBundle;

public class DiskUtilityApp extends Application {

    @Override
    public void start(Stage stage) {
        ResourceBundle bundle = ResourceBundle.getBundle("lang.messages", MainApp.CURRENT_LOCALE);
        TabPane tabs = new TabPane(
                new DiskOverviewTab(bundle),
                new DiskAnalysisTab(bundle),
                new DuplicateFinderTab(bundle),
                new DiskWiperTab(bundle),
                new SettingsTab(bundle),
                new HelpTab(bundle));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Scene scene = new Scene(tabs, 1000, 650);
        scene.getStylesheets().add(
                getClass().getResource("/styles/disk-utility.css").toExternalForm());

        stage.setTitle(bundle.getString("app.title"));
        stage.setScene(scene);
        stage.show();
    }
}