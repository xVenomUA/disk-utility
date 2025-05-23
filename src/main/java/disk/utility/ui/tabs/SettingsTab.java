package disk.utility.ui.tabs;

import disk.utility.MainApp;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class SettingsTab extends Tab {
    private final ComboBox<String> languageCombo;
    private final Button applyButton;
    private final Preferences prefs = MainApp.PREFS;
    private final ResourceBundle bundle;

    public SettingsTab(ResourceBundle bundle) {
        super(bundle.getString("tab.settings"));
        this.bundle = bundle;
        languageCombo = new ComboBox<>();
        languageCombo.getItems().addAll("English", "Українська");
        String currentLang = MainApp.CURRENT_LOCALE.getLanguage();
        languageCombo.setValue(currentLang.equals("uk") ? "Українська" : "English");

        applyButton = new Button(bundle.getString("button.apply"));
        applyButton.setOnAction(e -> applySettings());

        VBox content = new VBox(10, new Label(bundle.getString("label.language")), languageCombo, applyButton);
        setContent(content);
    }

    private void applySettings() {
        String selectedLang = languageCombo.getValue();
        String langCode = selectedLang.equals("Українська") ? "uk" : "en";
        prefs.put(MainApp.LANG_PREF, langCode);
        new Alert(Alert.AlertType.INFORMATION, bundle.getString("restart_message")).show();
    }
}