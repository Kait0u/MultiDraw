package wit.pap.multidraw;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class MultiDrawApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        Button btnPause, btnSave, btnClear, btnConnect;
        TextField tfHost, tfRoom;
        VBox root = new VBox();
        Scene scene = new Scene(root);

        ToolBar toolBar = new ToolBar(
                btnPause = new Button("Pause"),
                btnSave = new Button("Save"),
                new Separator(Orientation.VERTICAL),
                btnClear = new Button("Clear"),
                new Separator(Orientation.VERTICAL),
                new Label("Host: "),
                tfHost = new TextField(),
                new Label("Room ID: "),
                tfRoom = new TextField(),
                btnConnect = new Button("Connect")

        );
        root.getChildren().add(toolBar);


        Canvas canvas = new Canvas(1920, 1080);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });


        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(canvas);
        root.getChildren().add(scrollPane);


        primaryStage.setTitle("MultiDraw");
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(512);
        primaryStage.setMinWidth(512);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(768);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}