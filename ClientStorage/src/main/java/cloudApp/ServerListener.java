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
    private static String PATH = "";
    private static String fileName = "";
    private static ClientController controller;
    private static String authString;
    private static String userName;
    private static AuthorizationController authController;
    private static boolean activeSessionFlag = true;

    public ServerListener(String authString, AuthorizationController authController) {
        this.authString = authString;
        this.authController = authController;
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
                    authorizationServer(authString);
                    while(true) {
                        String msg = null;
                        try {
                            msg = inputStream.readUTF();
                            if(msg.startsWith("./logIn") || msg.startsWith("./singUp")) {
                                String[] param = msg.split("\\s");
                                if(param[1].equals("passed")) {
                                    authController.loadClientScreen();
                                    break;
                                } else {
                                    authController.failedAuthorization();
                                }
                            }
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    }
                    label:
                    while(true) {
                        try {
                            String message = inputStream.readUTF();
                            switch(message) {
                                case "quit":
                                    break label;
                                case "UPDATE":
                                    s = "";
                                    resultList();
                                    break;
                                case "DOWNLOAD":
                                    receivedFile(PATH, fileName);
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

    private static void authorizationServer(String authString) {
        sendMsg(authString);
    }

    public static synchronized boolean sendMsg(String msg) {
        try {
            if(socket == null || socket.isClosed()) {
                while(activeSessionFlag) {
                    launch();
                }
                return false;
            } else {
                outputStream.writeUTF(msg);
                return true;
            }
        } catch(IOException e) {
            System.out.println("OutputStream not found.");
            return false;
        }
    }

    public static boolean sendFile(String text, String PATH) {
        stat = false;
        String[] tokens = text.split("# ");
        String command = tokens[0];
        String fileName = tokens[1];
        File file = new File(PATH + fileName);
        try {
            outputStream.writeUTF(command);
            outputStream.writeUTF(fileName);
            byte[] bytes = new byte[1024];
            FileInputStream fis = new FileInputStream(file);
            OutputStream os = socket.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeLong(file.length());
            dos.flush();
            int count;
            while((count = fis.read(bytes)) > 0) {
                bos.write(bytes, 0, count);
            }
            bos.flush();
            fis.close();
            return true;
        } catch(IOException e) {
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

    public static void download() {
        try {
            stat = false;
            outputStream.writeUTF("./download");
            outputStream.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void receivedFile(String PATH, String nameFile) {
        try {
            outputStream.writeUTF(nameFile);
            outputStream.flush();
            long fileLength = inputStream.readLong();
            File file = new File(PATH + nameFile);
            if(file.createNewFile()) {
                try {
                    byte[] buf = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(file);
                    InputStream is = socket.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int count = 0;
                    while((fileLength > 0) && (count = bis.read(buf, 0, (int) Math.min(buf.length, fileLength))) > 0) {
                        fos.write(buf, 0, count);
                        fileLength -= count;
                    }
                    fos.close();
                    stat = true;
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void resultList() throws IOException {
        String f;
        do {
            f = inputStream.readUTF();
            s = s + f + "#";
            System.out.println(s);
        } while(! f.equals("OK"));
        stat = true;
    }

    public static String getS() {
        return s;
    }

    public static boolean isStat() {
        return ! stat;
    }

    public static void setPATH(String PATH) {
        ServerListener.PATH = PATH;
    }

    public static void setFileName(String fileName) {
        ServerListener.fileName = fileName;
    }

    public static void setController(ClientController contr) {
        controller = contr;
    }

    public static String getUserName() {
        return userName;
    }

    public static void setUserName(String userLogin) {
        userName = userLogin;
    }
}
