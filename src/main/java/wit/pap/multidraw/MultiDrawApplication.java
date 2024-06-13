package wit.pap.multidraw;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;

import java.io.IOException;

public class MultiDrawApplication extends Application {
    Button btnPauseResume, btnSave, btnClear, btnConnectDisconnect;
    TextField tfHost, tfRoom;
    Spinner<Integer> spnPenSize;
    ColorPicker colorPicker;
    Canvas canvas;

    @Override
    public void start(Stage primaryStage) throws IOException {
        VBox root = new VBox();
        Scene scene = new Scene(root);

        ToolBar toolBar = new ToolBar(
                btnPauseResume = new Button("Pause"),
                btnSave = new Button("Save"),
                new Separator(Orientation.VERTICAL),
                colorPicker = new ColorPicker(Color.BLACK),
                new Pane(),

                new Label("Pen Size: "),
                spnPenSize = new Spinner<Integer>() {{
                    setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            1, 50, 1
                    ));
                    setPrefWidth(80);
                }},
                new Pane(),
                btnClear = new Button("Clear"),
                new Separator(Orientation.VERTICAL),
                new Label("Host: "),
                tfHost = new TextField(),
                new Label("Room ID: "),
                tfRoom = new TextField(),
                btnConnectDisconnect = new Button("Connect")

        );
        root.getChildren().add(toolBar);

        canvas = new Canvas(1920, 1080);

        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                gc.setStroke(colorPicker.valueProperty().getValue());
                gc.setLineWidth(spnPenSize.getValue());
                gc.beginPath();
                gc.moveTo(e.getX(), e.getY());
                gc.stroke();
            } else if (e.getButton().equals(MouseButton.SECONDARY)) {
                clearCanvasPoint(e.getX(), e.getY());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                gc.lineTo(e.getX(), e.getY());
                gc.stroke();
            } else if (e.getButton().equals(MouseButton.SECONDARY)) {
                clearCanvasPoint(e.getX(), e.getY());
            }
        });

        btnClear.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent event) {
                clearCanvas();
            }
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

    private void clearCanvas() {
        if (canvas != null) {
            canvas.getGraphicsContext2D().clearRect(0,  0, canvas.getWidth(), canvas.getHeight());
        }
    }

    private void clearCanvasPoint(double x, double y) {
        int size = spnPenSize.getValue();
        int halfSize = size / 2;
        canvas.getGraphicsContext2D().clearRect(x - halfSize, y - halfSize, size, size);
    }

    public static void main(String[] args) {
        launch();
    }
}