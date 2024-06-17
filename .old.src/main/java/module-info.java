module wit.pap.multidraw {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

//    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;

//    opens wit.pap.multidraw to javafx.fxml;
//    exports wit.pap.multidraw;
    exports wit.pap.multidraw.client;
    opens wit.pap.multidraw.client to javafx.fxml;
    exports wit.pap.multidraw.client.gui;
    opens wit.pap.multidraw.client.gui to javafx.fxml;
}