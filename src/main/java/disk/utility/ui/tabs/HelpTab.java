package disk.utility.ui.tabs;

import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import java.util.ResourceBundle;

public class HelpTab extends Tab {
    private final ResourceBundle bundle;

    public HelpTab(ResourceBundle bundle) {
        super(bundle.getString("tab.help"));
        this.bundle = bundle;

        TextArea helpText = new TextArea(bundle.getString("help.text"));
        helpText.setEditable(false);
        helpText.setWrapText(true);
        setContent(helpText);
    }
}