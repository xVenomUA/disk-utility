package disk.utility;

import disk.utility.ui.DiskUtilityApp;
import javafx.application.Application;

import java.util.Locale;
import java.util.prefs.Preferences;

public class MainApp {
    public static final Preferences PREFS = Preferences.userNodeForPackage(MainApp.class);
    public static final String LANG_PREF = "language";
    public static Locale CURRENT_LOCALE;

    public static void main(String[] args) {
        String lang = PREFS.get(LANG_PREF, "en");
        CURRENT_LOCALE = new Locale(lang);
        Application.launch(DiskUtilityApp.class, args);
    }
}
