package cloudApp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class ClientMain extends Application {

@Override
public void start(Stage primaryStage) throws Exception{
    Parent root = FXMLLoader.load(getClass().getResource("/view/authorization.fxml"));
    primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/cloud.png")));
    primaryStage.setTitle("CloudStorage");
    Scene scene = new Scene(root, 400, 300);
    primaryStage.setMinWidth(400);
    primaryStage.setMinHeight(300);
    primaryStage.setResizable(false);
    primaryStage.setScene(scene);
    primaryStage.show();
}

    @Override
    public void stop() {
    }

    public static void main(String[] args) {
        launch(args);
    }
}

