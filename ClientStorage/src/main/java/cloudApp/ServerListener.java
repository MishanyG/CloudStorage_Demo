package cloudApp;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class ServerListener {
    private static final String SERVER_HOSTNAME = "127.0.0.1";
    private static final int SERVER_PORT = 8189;
    public static Socket socket = null;
    private static DataInputStream inputStream;
    private static DataOutputStream outputStream;
    private static ClientController controller;

    public ServerListener() {
        launch();
    }

    public static void launch() {
        try {
            socket = new Socket(SERVER_HOSTNAME, SERVER_PORT);
            System.out.println("You joining cloud storage.");
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            Thread netThread = new Thread(() -> {
                try {
                    try {
                        Thread.sleep(1000);
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    }
                    while(true) {
                        try {
                            String message = inputStream.readUTF();
                            if(message.equals("quit")) {
                                break;
                            }
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }

                } finally {
                    closeConnection();
                }
            });
            netThread.setDaemon(true);
            netThread.start();

        } catch(UnknownHostException e) {
            System.out.println("Server not found.");
        } catch(IOException ex) {
            System.out.println("Couldn't connect to server.");
        }
    }

    public static void closeConnection() {
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch(IOException e) {
            System.out.println("Couldn't close connection.");
        }
    }

    public static boolean sendFile(String text, String PATH) {
        String[] tokens = text.split(" ");
        String command = tokens[0];
        String fileName = tokens[1];
        File file = new File(PATH + fileName);
        try(FileInputStream fis = new FileInputStream(file)) {
            outputStream.writeUTF(command);
            outputStream.writeUTF(fileName);
            outputStream.writeLong(file.length());
            byte[] buffer = new byte[256];
            int read = 0;
            while((read = fis.read(buffer)) != - 1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
