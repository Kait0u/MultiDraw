package wit.pap.multidraw.client.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.stage.StageStyle;
import javafx.util.Duration;
import wit.pap.multidraw.client.gui.utilities.Alerts;
import wit.pap.multidraw.client.gui.widgets.LayeredImageStack;
import wit.pap.multidraw.client.gui.widgets.PannableScrollPane;
import wit.pap.multidraw.client.utils.TCPHandler;
import wit.pap.multidraw.shared.BgraImage;
import wit.pap.multidraw.shared.communication.ClientCommands;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.globals.Globals;
import wit.pap.multidraw.shared.LayeredImage;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class MultiDrawApplication extends Application {
    private Stage primaryStage;
    private Button btnAbout, btnSave, btnClear, btnConnectDisconnect;
    private HBox connectionForm;
    private TextField tfHost, tfHostPort, tfNickname, tfRoom;
    private Spinner<Integer> spnPenSize;
    private ColorPicker colorPicker;

    private LayeredImage layeredImage;
    private LayeredImageStack imageStack;
    private Canvas canvas;
    private ImageView bgImageView, mgImageView;

    TCPHandler tcpHandler;


    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;
        VBox root = new VBox();
        Scene scene = new Scene(root);

        ToolBar toolBar = new ToolBar(
                btnSave = new Button("Save"),
                btnAbout = new Button("About"),
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

                                Pattern validPattern = Pattern.compile("[0-9]*");
                                setTextFormatter(new TextFormatter<>(change -> {
                                    String newText = change.getControlNewText();
                                    if (validPattern.matcher(newText).matches())
                                        return change;
                                    return null;
                                }));
                            }},
                            new Label("Nickname: "),
                            tfNickname = new TextField() {{
                                Pattern validPattern = Pattern.compile("[a-zA-Z0-9_]*");
                                setTextFormatter(new TextFormatter<>(change -> {
                                    String newText = change.getControlNewText();
                                    if (newText.length() > Globals.MAX_NICKNAME_LENGTH)
                                        return null;
                                    if (validPattern.matcher(newText).matches())
                                        return change;

                                    return null;
                                }));

                            }},
                            new Label("Room ID: "),
                            tfRoom = new TextField() {{
                                Pattern validPattern = Pattern.compile("[a-zA-Z0-9_]*");
                                setTextFormatter(new TextFormatter<>(change -> {
                                    String newText = change.getControlNewText();
                                    if (newText.length() > Globals.MAX_ROOMNAME_LENGTH)
                                        return null;
                                    if (validPattern.matcher(newText).matches())
                                        return change;

                                    return null;
                                }));
                            }}
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

        btnSave.setOnAction(event -> savePNG());
        btnAbout.setOnAction(event -> showAboutWindow(primaryStage));
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

    @Override
    public void stop() {
        if (!tcpHandler.getIsConnectionDead()) {
            stopHandler();
        } else {
            try {
                tcpHandler.interrupt();
            } catch (Exception ignored) { }
        }
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

            reset();
            clearCanvas();


        } catch (IOException e) {
            Alerts.showErrorAlert(e.toString(), e.getMessage());
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
        tcpHandler.setCbGetCanvasImage(() -> Platform.runLater(this::snapshotCanvas));
        tcpHandler.setCbSetMiddleGround(im -> Platform.runLater(() -> imageStack.setMgImage(im)));
    }

    private void disconnect() {
        stopHandler();
        freeForm();
    }

    private void stopHandler() {
        if (tcpHandler != null)
            tcpHandler.stopHandler();
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

    public void snapshotCanvas() {
        if (canvas == null) return;

        WritableImage img = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        SnapshotParameters snapshotParameters = new SnapshotParameters();
        snapshotParameters.setFill(Color.TRANSPARENT);
        final TCPHandler finalTcpHandler = tcpHandler;

        canvas.snapshot(snapshotResult -> {
            BgraImage image = BgraImage.fromFXImage(snapshotResult.getImage());
            finalTcpHandler.setImage(image);
            return null;
        }, snapshotParameters, img);
    }

    public void savePNG() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save As PNG");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);

        File outFile = fileChooser.showSaveDialog(primaryStage);
        if (outFile != null) {
            synchronized (imageStack) {
                imageStack.saveAsPNG(outFile);
            }
        }
    }

    private void showAboutWindow(Stage owner) {
        Stage aboutStage = new Stage();

        aboutStage.initOwner(owner);
        aboutStage.initModality(Modality.WINDOW_MODAL);
        aboutStage.initStyle(StageStyle.UTILITY);
        aboutStage.setTitle("About");

        Image image = new Image(MultiDrawApplication.class.getResourceAsStream("/icon_big.png"));
        ImageView imageView = new ImageView(image);

        Label headerLabel = new Label("MultiDraw");
        headerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label infoLabel = new Label(
                new StringBuilder("A client-server app that allows users to draw on a whiteboard together.")
                .append("\n")
                .append("\nCredit project for Client-Server Programming class.")
                .append("\n\n")
                .append("Author: Kait0u (Jakub Jaworski [20318])")
                .append("\n")
                .append("Icon by: Nexonus (Jan Konarski)")
                .append("\n")
                .append("2024 - WIT Academy, Warsaw")
                .toString()
        );

        infoLabel.setStyle("-fx-text-alignment: center;");

        Button okButton = new Button("OK");
        okButton.setOnAction(e -> aboutStage.close());

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(imageView, headerLabel, infoLabel, okButton);

        Scene scene = new Scene(layout, 400, 400);
        aboutStage.setScene(scene);
        aboutStage.setResizable(false);
        aboutStage.showAndWait();
    }

    // ---------


    public LayeredImage getLayeredImage() {
        return layeredImage;
    }
}