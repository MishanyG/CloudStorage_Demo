package cloudApp;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuthorizationController implements Initializable {
    public TextField loginField;
    public PasswordField passField;
    private ServerListener serverListener;
    private ClientController controller;

    final PseudoClass errorClass = PseudoClass.getPseudoClass("error");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> loginField.requestFocus());
    }

    public void logIn(ActionEvent actionEvent) {
        String login = loginField.getText();
        String pass = passField.getText();

        serverListenerLauncher("./logIn " + login + " " + pass);
        ServerListener.setUserName(login);
    }

    public void signUp(ActionEvent actionEvent) {
        String login = loginField.getText();
        String pass = passField.getText();

        serverListenerLauncher("./singUp " + "petr " + login + " " + pass);
        ServerListener.setUserName(login);
    }

    public void failedAuthorization() {
        loginField.pseudoClassStateChanged(errorClass, true);
        passField.pseudoClassStateChanged(errorClass, true);
    }

    public void loadClientScreen() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/sample.fxml"));
                    Parent window = (Pane) fxmlLoader.load();
                    controller = fxmlLoader.getController();
                    ServerListener.setController(controller);
                    Stage stage = (Stage) loginField.getScene().getWindow();
                    stage.setResizable(true);
                    stage.setOnCloseRequest(e -> {
                        Platform.exit();
                        System.exit(0);
                    });
                    Scene scene = new Scene(window);
                    stage.setScene(scene);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void serverListenerLauncher(String authString) {
        new Thread( () -> {
            serverListener = new ServerListener(authString, this);
        }).start();
    }
}
