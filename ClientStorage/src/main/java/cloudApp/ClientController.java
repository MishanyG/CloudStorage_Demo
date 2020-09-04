package cloudApp;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    private static DataInputStream is;
    private static DataOutputStream os;
    public ListView <String> listFolder;
    public ListView <String> listFiles;
    public ListView <String> listOnFiles;
    public Button sendFile;
    public Button openFile;
    public Button deleteFile;
    public TextField textFolder;
    public TextField textFile;
    public HBox cloudField;
    private ServerListener serverListener;
    private String PATH = "";
    private boolean pathFull = false;

    public static void stop() {
        try {
            os.writeUTF("quit");
            os.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(ActionEvent actionEvent) throws IOException {
        if(! pathFull) {
            if(ServerListener.sendFile("./upload " + textFile.getText(), PATH + textFolder.getText() + "\\"))
                textFile.setText("File uploaded!");
        } else {
            if(ServerListener.sendFile("./upload " + textFile.getText(), PATH + "\\"))
                textFile.setText("File uploaded!");
        }
    }

    public void openFile(ActionEvent actionEvent) {
        String pathFile = "";
        if(! pathFull)
            pathFile = PATH + textFolder.getText() + "\\" + textFile.getText();
        else
            pathFile = PATH + "\\" + textFile.getText();
        Desktop desktop = null;
        if(Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        }
        try {
            assert desktop != null;
            desktop.open(new File(pathFile));
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void deleteFile(ActionEvent actionEvent) {

    }

    public void readFiles(File baseDirectory) {
        if(baseDirectory.isDirectory()) {
            for(File file : Objects.requireNonNull(baseDirectory.listFiles())) {
                if(file.isFile()) {
                    listFiles.getItems().add(file.getName());
                }
            }
        }
    }

    public void setListOnFiles(ListView <String> listOnFiles) {
        this.listOnFiles = listOnFiles;
    }

    public void readFolders(File baseDirectory) {
        if(baseDirectory.isDirectory()) {
            for(File file : Objects.requireNonNull(baseDirectory.listFiles())) {
                if(! file.isFile()) {
                    listFolder.getItems().add(file.getName());
                }
            }
        }
    }

    public void readDisk() {
        File[] roots = File.listRoots();
        for(File root : roots)
            listFolder.getItems().add(root.getAbsolutePath());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textFile.setOnAction(this :: openFile);
        readDisk();
        new Thread(() -> {
            listFolder.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if(listFolder.getItems().size() != 0) {
                    textFolder.setText(newValue);
                    listFiles.getItems().clear();
                    readFiles(new File(PATH + newValue));
                    pathFull = false;
                    listFiles.getSelectionModel().selectedItemProperty().addListener((observableF, oldValueF, newValueF) -> textFile.setText(newValueF));
                }
            });
            listFolder.setOnMouseClicked(click -> {
                if(click.getClickCount() == 2) {
                    PATH = PATH + textFolder.getText() + "\\";
                    pathFull = true;
                    listFolder.getItems().clear();
                    readFolders(new File(PATH));
                } else if(click.getButton() == MouseButton.SECONDARY) {
                    File f = new File(PATH);
                    PATH = f.getParentFile() + "\\";
                    listFolder.getItems().clear();
                    if(! PATH.equals("null\\")) readFolders(new File(PATH));
                    else {
                        readDisk();
                        PATH = "";
                    }
                }
            });
            serverListener = new ServerListener();
        }).start();
    }
}