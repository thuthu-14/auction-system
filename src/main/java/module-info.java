module io.auctionsystem {
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.controls;

    opens io.auctionsystem.controllers to javafx.fxml, javafx.graphics;

    exports io.auctionsystem.controllers;
}