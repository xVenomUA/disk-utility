// src/main/java/disk/utility/ui/components/DirectoryPicker.java
package disk.utility.ui.components;

import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.scene.layout.HBox;
import java.io.File;

public class DirectoryPicker extends HBox {
    private final TextField path = new TextField();

    public DirectoryPicker(String placeholder) {
        path.setPromptText(placeholder);
        Button browse = new Button("Browse…");
        browse.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            File dir = chooser.showDialog(getScene().getWindow());
            if (dir != null)
                path.setText(dir.getAbsolutePath());
        });
        setSpacing(6);
        getChildren().addAll(path, browse);
    }

    public String getPath() {
        return path.getText().trim();
    }
}
