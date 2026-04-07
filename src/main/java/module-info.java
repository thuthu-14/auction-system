module io.auctionsystem {
    requires javafx.controls;
    requires javafx.fxml;


    opens io.auctionsystem to javafx.fxml;
    opens io.auctionsystem.controllers to javafx.fxml;

    exports io.auctionsystem;
    exports io.auctionsystem.controllers;
}