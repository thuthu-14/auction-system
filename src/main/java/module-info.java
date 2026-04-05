module org.example.demo2 {
    requires javafx.controls;
    requires javafx.fxml;

    opens io.auctionsystem to javafx.fxml, javafx.graphics;

    exports io.auctionsystem;
}