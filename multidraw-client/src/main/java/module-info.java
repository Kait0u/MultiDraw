module multidraw.client {
    requires transitive java.desktop;

    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.swing;
    requires multidraw.shared;

    exports wit.pap.multidraw.client.gui to javafx.graphics;
    exports wit.pap.multidraw.client;
}