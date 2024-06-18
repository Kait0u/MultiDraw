package wit.pap.multidraw.client.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;

import javafx.util.Duration;
import wit.pap.multidraw.client.gui.utilities.Alerts;
import wit.pap.multidraw.client.gui.widgets.LayeredImageStack;
import wit.pap.multidraw.client.gui.widgets.PannableScrollPane;
import wit.pap.multidraw.client.utils.TCPHandler;
import wit.pap.multidraw.shared.communication.ClientCommands;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.globals.Globals;
import wit.pap.multidraw.shared.LayeredImage;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MultiDrawApplication extends Application {
    Button btnPauseResume, btnSave, btnClear, btnConnectDisconnect;
    HBox connectionForm;
    TextField tfHost, tfHostPort, tfNickname, tfRoom;
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
                btnSave = new Button("Save"),
                new Separator(Orientation.VERTICAL),
                connectionForm = new HBox() {{
                    setSpacing(5);
                    setAlignment(Pos.CENTER);
                    getChildren().addAll(
                            new Label("Host: "),
                            tfHost = new TextField(),
                            new Label("Port: "),
                            tfHostPort = new TextField() {{
                                setPrefWidth(80);
                            }},
                            new Label("Nickname: "),
                            tfNickname = new TextField(),
                            new Label("Room ID: "),
                            tfRoom = new TextField()
                    );
                }},
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

        PannableScrollPane scrollPane = new PannableScrollPane();
        scrollPane.setContent(imageStack);
        root.getChildren().add(scrollPane);

        ToolBar toolBarBottom = new ToolBar(
                colorPicker = new ColorPicker(Color.BLACK),
                new Pane(),

                new Label("Size: "),
                spnPenSize = new Spinner<Integer>() {{
                    setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                            1, 50, 5
                    ));
                    setPrefWidth(80);
                }},
                new Pane(),
                btnClear = new Button("Clear")
        );

        root.getChildren().add(toolBarBottom);

        btnClear.setOnAction(event -> clearCanvas());
        btnConnectDisconnect.setOnAction(event -> connect());

        primaryStage.setTitle("MultiDraw");
        primaryStage.getIcons().add(new Image(MultiDrawApplication.class.getResourceAsStream("/icon.png")));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(Globals.WINDOW_MIN_W);
        primaryStage.setMinHeight(Globals.WINDOW_MIN_H);
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
            String nicknmame = tfNickname.getText();
            String roomName = tfRoom.getText();

            tcpHandler = new TCPHandler(address, port);
            configureTcpHandlerCbs();

            tcpHandler.queueMessages(
                    new ClientMessage(ClientCommands.SET_NICKNAME, nicknmame.getBytes()),
                    new ClientMessage(ClientCommands.JOIN_CREATE_ROOM, roomName.getBytes())
            );

            tcpHandler.start();
            connectionForm.setDisable(true);
            btnConnectDisconnect.setText("Disconnect");
            btnConnectDisconnect.setOnAction(e -> disconnect());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureTcpHandlerCbs() {
        if (tcpHandler == null) return;

        tcpHandler.setCbOnClose(() -> Platform.runLater(this::freeForm));
        tcpHandler.setCbShowError(s -> Platform.runLater(
                () -> Alerts.showErrorAlert("The following error has occurred:", s)
            )
        );
        tcpHandler.setCbShowMessage(s -> Platform.runLater(
                () -> Alerts.showInfoAlert("The following has occurred:", s)
            )
        );
    }

    private void disconnect() {
        tcpHandler.stopHandler();
        freeForm();
    }

    private void freeForm() {
        btnConnectDisconnect.setDisable(true);

        Timeline timeline = new Timeline (
                new KeyFrame(Duration.seconds(5), evt -> {
                    connectionForm.setDisable(false);
                    btnConnectDisconnect.setText("Connect");
                    btnConnectDisconnect.setOnAction(e -> connect());
                    btnConnectDisconnect.setDisable(false);
                }));

        timeline.play();
    }

    // ---------


    public LayeredImage getLayeredImage() {
        return layeredImage;
    }
}