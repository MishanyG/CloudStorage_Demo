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
    private static String s = "";
    private static boolean stat = false;

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
                            } else if(message.equals("UPDATE")) {
                                s = "";
                                resultList();
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
        stat = false;
        String[] tokens = text.split("# ");
        String command = tokens[0];
        String fileName = tokens[1];
        File file = new File(PATH + fileName);
        try{
            outputStream.writeUTF(command);
            outputStream.writeUTF(fileName);;
            byte[] bytes = new byte[1024];
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = socket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeLong(file.length());
            dos.flush();
            int count;
            while ((count = fis.read(bytes)) > 0) {
                bos.write(bytes, 0, count);
            }
            bos.flush();
            fis.close();
            return true;
        }catch(IOException e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteFile(String text) {
        stat = false;
        try {
            outputStream.writeUTF("./delete");
            outputStream.writeUTF(text);
            outputStream.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static void resultList() throws IOException {
        String f;
        do {
            f = inputStream.readUTF();
            s = s + f + "#";
            System.out.println(s);
        } while(!f.equals("OK"));
        stat = true;
    }

    public static String getS() {
        return s;
    }

    public static boolean isStat() {
        return ! stat;
    }
}
