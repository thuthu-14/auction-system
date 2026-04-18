package io.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
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
        String user = username.getText() != null ? username.getText().trim() : "";
        String pass = "";
        if (mkHide.isVisible()) {
            pass = mkHide.getText() != null ? mkHide.getText().trim() : "";
        } else {
            pass = mkShow.getText() != null ? mkShow.getText().trim() : "";
        }

        System.out.println("--- CHECK ĐĂNG NHẬP ---");
        System.out.println("Tài khoản đang lấy được: [" + user + "]");
        System.out.println("Mật khẩu đang lấy được: [" + pass + "]");
        System.out.println("-----------------------");
        if (user.equals("admin") && pass.equals("123")) {
            try {
                URL fxmlLocation = getClass().getResource("/io/auctionsystem/Home.fxml");

                if (fxmlLocation == null) {
                    System.err.println("Lỗi: Không tìm thấy Home.fxml tại /io.auctionsystem/Home.fxml");
                    return;
                }

                FXMLLoader fxmlLoader = new FXMLLoader(fxmlLocation);
                Parent root = fxmlLoader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);

                stage.setScene(scene);
                stage.setTitle("Trang chủ hệ thống đấu giá");
                stage.setMaximized(true);
                stage.show();

            } catch (Exception e) {
                System.err.println("Lỗi khi load màn hình Home!");
                e.printStackTrace();
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

    @FXML
    private void switchToSignup(ActionEvent event) throws IOException {
        URL signupLocation = getClass().getResource("/io/auctionsystem/signup.fxml");

        if (signupLocation != null) {
            Parent root = FXMLLoader.load(signupLocation);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } else {
            System.err.println("Lỗi: Không tìm thấy signup.fxml tại /io.auctionsystem/signup.fxml");
        }
    }
}