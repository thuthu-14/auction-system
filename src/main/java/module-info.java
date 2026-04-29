module io.auctionsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires java.sql;

    exports util;
    opens util to javafx.fxml, com.google.gson;

    exports navigation;
    opens navigation to javafx.fxml;

    exports client.controller;
    opens client.controller to javafx.fxml;

    exports client;
    opens client to javafx.fxml;
}