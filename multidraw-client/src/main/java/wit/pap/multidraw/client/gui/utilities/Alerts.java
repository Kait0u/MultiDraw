package wit.pap.multidraw.client.gui.utilities;

import javafx.scene.control.Alert;

public class Alerts {
    private static void showAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showErrorAlert(String title, String header, String content) {
        showAlert(title, header, content, Alert.AlertType.ERROR);
    }

    public static void showErrorAlert(String header, String content) {
        showErrorAlert("Error!", header, content);
    }

    public static void showWarningAlert(String title, String header, String content) {
        showAlert(title, header, content, Alert.AlertType.WARNING);
    }

    public static void showWarningAlert(String header, String content) {
        showWarningAlert("Warning!", header, content);
    }

    public static void showInfoAlert(String title, String header, String content) {
        showAlert(title, header, content, Alert.AlertType.INFORMATION);
    }

    public static void showInfoAlert(String header, String content) {
        showAlert("Information", header, content, Alert.AlertType.INFORMATION);
    }

}
