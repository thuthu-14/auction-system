package io.auctionsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML
    private TextField username;
    @FXML
    private PasswordField mkHide;
    @FXML
    private TextField mkShow;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        mkShow.setVisible(false);
        mkShow.setManaged(false);
    }

    @FXML
    void handleLogin(ActionEvent event) {
        String user = username.getText();
        String pass = mkHide.isVisible() ? mkHide.getText() : mkShow.getText();

        if (user.equals("admin") && pass.equals("123")) {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("home.fxml"));
                Scene scene = new Scene(fxmlLoader.load());
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Trang chủ hệ thống đấu giá");
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Không tìm thấy file home.fxml!");
            }
        } else {
            System.out.println("Sai tài khoản hoặc mật khẩu!");
        }
    }

    @FXML
    void showPassword(MouseEvent event) {
        mkShow.setText(mkHide.getText());
        mkShow.setVisible(true);
        mkShow.setManaged(true);

        mkHide.setVisible(false);
        mkHide.setManaged(false);
    }

    @FXML
    void hidePassword(MouseEvent event) {
        mkHide.setText(mkShow.getText());
        mkHide.setVisible(true);
        mkHide.setManaged(true);

        mkShow.setVisible(false);
        mkShow.setManaged(false);
    }
}