package wit.pap.multidraw.client.gui;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;

import wit.pap.multidraw.client.gui.widgets.LayeredImageStack;
import wit.pap.multidraw.client.gui.widgets.PannableScrollPane;
import wit.pap.multidraw.client.utils.TCPHandler;
import wit.pap.multidraw.shared.communication.ClientCommands;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.globals.Globals;
import wit.pap.multidraw.shared.LayeredImage;
import wit.pap.multidraw.shared.communication.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MultiDrawApplication extends Application {
    Button btnPauseResume, btnSave, btnClear, btnConnectDisconnect;
    TextField tfHost, tfHostPort, tfRoom;
    Spinner<Integer> spnPenSize;
    ColorPicker colorPicker;

    LayeredImage layeredImage;
    LayeredImageStack imageStack;
    Canvas canvas;
    ImageView bgImageView, mgImageView;

    TCPHandler tcpHandler;

    @Override
    public void start(Stage primaryStage) throws IOException {
        VBox root = new VBox();
        Scene scene = new Scene(root);

        ToolBar toolBar = new ToolBar(
//                btnPauseResume = new Button("Pause"),
                btnSave = new Button("Save"),
                new Separator(Orientation.VERTICAL),
                colorPicker = new ColorPicker(Color.BLACK),
                new Pane(),

                new Label("Pen Size: "),
                spnPenSize = new Spinner<Integer>() {{
                    setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            1, 50, 5
                    ));
                    setPrefWidth(80);
                }},
                new Pane(),
                btnClear = new Button("Clear"),
                new Separator(Orientation.VERTICAL),
                new Label("Host: "),
                tfHost = new TextField(),
                new Label("Port: "),
                tfHostPort = new TextField(),
                new Label("Room ID: "),
                tfRoom = new TextField(),
                btnConnectDisconnect = new Button("Connect")

        );
        root.getChildren().add(toolBar);

        layeredImage = new LayeredImage();
        imageStack = new LayeredImageStack(layeredImage);

        canvas = imageStack.getFgCanvas();

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

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

        btnClear.setOnAction(event -> clearCanvas());

        btnConnectDisconnect.setOnAction(event -> connect());


        PannableScrollPane scrollPane = new PannableScrollPane();
        scrollPane.setContent(imageStack);
        root.getChildren().add(scrollPane);


        primaryStage.setTitle("MultiDraw");
        primaryStage.getIcons().add(new Image(MultiDrawApplication.class.getResourceAsStream("/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setMinHeight(Globals.WINDOW_MIN_DIM);
        primaryStage.setMinWidth(Globals.WINDOW_MIN_DIM);
        primaryStage.setWidth(Globals.WINDOW_INITIAL_WIDTH);
        primaryStage.setHeight(Globals.WINDOW_INITIAL_HEIGHT);
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

    private void reset() {
        layeredImage = new LayeredImage();
        imageStack.setLayeredImage(layeredImage);
    }

    private void connect() {
        try {
            InetAddress address = InetAddress.getByName(tfHost.getText());
            Integer port = Integer.valueOf(tfHostPort.getText());
            String roomName = tfRoom.getText();

            tcpHandler = new TCPHandler(address, port);
            tcpHandler.start();

            ClientMessage msg = new ClientMessage(ClientCommands.JOIN_CREATE_ROOM, roomName.getBytes());
            tcpHandler.queueMessage(msg);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------


    public LayeredImage getLayeredImage() {
        return layeredImage;
    }
}