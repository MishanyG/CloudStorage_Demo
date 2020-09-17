package cloudApp;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
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
    public Button downloadFile;
    public HBox cloudField;
    public Label statusBarFile;
    public Label statusBarFold;
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
            if(ServerListener.sendFile("./upload# " + statusBarFile.getText(), PATH + statusBarFold.getText() + "\\"))
                statusBarFile.setText("File uploaded!");
        } else {
            if(ServerListener.sendFile("./upload# " + statusBarFile.getText(), PATH + "\\"))
                statusBarFile.setText("File uploaded!");
        }
        sendFile.setDisable(true);
        openFile.setDisable(true);
        deleteFile.setDisable(true);
        deleteFile.setDisable(true);
        while(ServerListener.isStat()) listOnFiles.getItems().clear();
        setListOnFiles();
    }

    public void openFile(ActionEvent actionEvent) {
        String pathFile = "";
        if(! pathFull) pathFile = PATH + statusBarFold.getText() + "\\" + statusBarFile.getText();
        else pathFile = PATH + "\\" + statusBarFile.getText();
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
        if(ServerListener.deleteFile(statusBarFile.getText())) {
            deleteFile.setDisable(true);
            downloadFile.setDisable(true);
            while(ServerListener.isStat()) listOnFiles.getItems().clear();
            setListOnFiles();
        }
        statusBarFile.setText("File deleted from repository!");
    }

    public void downloadFile(ActionEvent actionEvent) {
        ServerListener.download();
        ServerListener.setPATH(PATH + statusBarFold.getText() + "\\");
        ServerListener.setFileName(statusBarFile.getText());
        deleteFile.setDisable(true);
        downloadFile.setDisable(true);
        while(ServerListener.isStat()) listFiles.getItems().clear();
        readFiles(new File(PATH + statusBarFold.getText()));
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

    public void setListOnFiles() {
        deleteFile.setDisable(true);
        downloadFile.setDisable(true);
        while(true) {
            String[] s = ServerListener.getS().split("#");
            if(s[s.length - 1].equals("OK")) {
                for(int i = 0; i < s.length - 1; i++) {
                    listOnFiles.getItems().add(s[i]);
                }
                break;
            }
        }
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
        sendFile.graphicProperty().setValue(new ImageView("/images/send.png"));
        sendFile.setDisable(true);
        openFile.graphicProperty().setValue(new ImageView("/images/open.png"));
        openFile.setDisable(true);
        deleteFile.graphicProperty().setValue(new ImageView("/images/delete.png"));
        deleteFile.setDisable(true);
        downloadFile.graphicProperty().setValue(new ImageView("/images/download.png"));
        downloadFile.setDisable(true);
        readDisk();
        new Thread(() -> {
            listFolder.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                sendFile.setDisable(true);
                openFile.setDisable(true);
                deleteFile.setDisable(true);
                downloadFile.setDisable(true);
                if(listFolder.getItems().size() != 0) {
                    statusBarFold.setText(newValue);
                    listFiles.getItems().clear();
                    readFiles(new File(PATH + newValue));
                    pathFull = false;
                    listFiles.getSelectionModel().selectedItemProperty().addListener((observableF, oldValueF, newValueF) -> {
                        statusBarFile.setText(newValueF);
                        sendFile.setDisable(false);
                        openFile.setDisable(false);
                        deleteFile.setDisable(true);
                        downloadFile.setDisable(true);
                    });
                }
            });
            listFolder.setOnMouseClicked(click -> {
                sendFile.setDisable(true);
                openFile.setDisable(true);
                deleteFile.setDisable(true);
                downloadFile.setDisable(true);
                if(click.getClickCount() == 2) {
                    PATH = PATH + statusBarFold.getText() + "\\";
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
            listOnFiles.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                sendFile.setDisable(true);
                openFile.setDisable(true);
                deleteFile.setDisable(false);
                downloadFile.setDisable(false);
                statusBarFile.setText(newValue);
            });
            setListOnFiles();
        }).start();
    }
}