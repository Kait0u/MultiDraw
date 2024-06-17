package wit.pap.multidraw.client.gui.widgets;

import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class PannableScrollPane extends ScrollPane {

    public PannableScrollPane() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.MIDDLE) setPannable(true);
        });
        setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.MIDDLE) setPannable(false);
        });
    }
}
