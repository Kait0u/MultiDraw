module multidraw.client {
    requires javafx.graphics;
    requires javafx.controls;
    requires multidraw.shared;

    exports wit.pap.multidraw.client.gui to javafx.graphics;
    exports wit.pap.multidraw.client;
}