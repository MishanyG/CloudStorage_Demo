import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientHandler implements Runnable {

    private static final String PATH = "ServerStorage/OutFiles/";
    private String name;
    private final ServerMain server;
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private Connection connection;
    private PasswordHandler passwordHandler;
    private final List <String> commands = Arrays.asList("./upload", "./download", "./delete");

    public ClientHandler(ServerMain server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            this.name = "";
            ServerMain.executorServiceClient.execute(new Thread(this));
        } catch(IOException e) {
            throw new RuntimeException("Problems when you create handler of client.");
        }
    }

    public synchronized void readMessages() throws IOException {
        readFiles(new File(PATH));
        while(true) {
            String msgFromClient = inputStream.readUTF();
            for(String command : commands) {
                if(msgFromClient.startsWith(command)) {
                    switch(command) {
                        case "./upload":
                            receivedFile();
                            readFiles(new File(PATH));
                            ServerMain.LOGGER.info("File received! OK");
                            break;
                        case "./download":
                            sendFile();
                            break;
                        case "./delete":
                            delete();
                            readFiles(new File(PATH));
                            break;
                    }
                }
            }
        }
    }

    private void receivedFile() {
        try {
            ServerMain.LOGGER.info("Started getting file...");
            String fileName = inputStream.readUTF();
            long fileLength = inputStream.readLong();
            File file = new File(PATH + fileName);
            if(file.createNewFile()) {
                try{
                    byte [] buf  = new byte [1024];
                    FileOutputStream fos = new FileOutputStream(file);
                    InputStream is = socket.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    int count;
                    while ((fileLength > 0) && (count = bis.read(buf, 0, (int)Math.min(buf.length, fileLength))) > 0){
                        fos.write(buf, 0, count);
                        fileLength -= count;
                    }
                    fos.close();
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void readFiles(File baseDirectory) throws IOException {
        outputStream.writeUTF("UPDATE");
        String list;
        if(baseDirectory.isDirectory()) {
            for(File file : Objects.requireNonNull(baseDirectory.listFiles())) {
                if(file.isFile()) {
                    list = file.getName();
                    outputStream.writeUTF(list);
                    System.out.println(list);
                }
            }
            outputStream.writeUTF("OK");
            outputStream.flush();
        }
    }

    private void delete() {
        try {
            String fileName = inputStream.readUTF();
            File file = new File(PATH + fileName);
            if(file.delete()) {
                ServerMain.LOGGER.info(PATH + fileName + " file deleted!");
            } else ServerMain.LOGGER.info(PATH + fileName + " file not found");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile() throws IOException {
        outputStream.writeUTF("DOWNLOAD");
        outputStream.flush();
        String fileName = inputStream.readUTF();
        File file = new File(PATH + fileName);
        try{
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
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private boolean logIn(String str) {
        passwordHandler = new PasswordHandler();
        String[] parts = str.split("\\s");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:ServerStorage/src/main/resources/CloudStorage.db");
            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM" +
                    " Users WHERE login = ?");
            ps.setString(1, parts[1]);
            ResultSet rs = ps.executeQuery();
            connection.commit();
            if (passwordHandler.validatePassword(parts[2], rs.getInt(4), rs.getString(5), rs.getString(6))) {
                sendMessage("./auth ok " + parts[1]);
                name = parts[1];
                ServerMain.LOGGER.info(name + " connected.");
                server.subscribe(this);
                return true;
            }
            connection.close();
        } catch (NoSuchAlgorithmException | SQLException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean signUp(String str) throws InvalidKeySpecException, NoSuchAlgorithmException {
        passwordHandler = new PasswordHandler();
        String[] parts = str.split("\\s");
        String hashString = passwordHandler.createHash(parts[3]);
        String[] param = hashString.split(":");
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:ServerStorage/src/main/resources/CloudStorage.db");
            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement("INSERT INTO Users (nickname, login, iteration, salt, hash) VALUES (?,?,?,?,?)");
            ps.setString(1, parts[1]);
            ps.setString(2, parts[2]);
            ps.setString(3, param[0]);
            ps.setString(4, param[1]);
            ps.setString(5, param[2]);
            ps.executeUpdate();
            connection.commit();
            connection.close();
        } catch (SQLException throwables) {
            return false;
        }
        return true;
    }

    public boolean authentication() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        String str;
        while ((str = inputStream.readUTF()) != null) {
            if (str.startsWith("./singUp")) {
                if (signUp(str)) {
                    sendMessage("./singUp passed");
                    break;
                } else {
                    sendMessage("./singUp failed");
                }
            }
            if (str.startsWith("./logIn")) {
                if (logIn(str)) {
                    sendMessage("./logIn passed");
                    break;
                } else {
                    sendMessage("./logIn failed");
                }
            }
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void sendMessage(String msg) {
        try {
            outputStream.writeUTF(msg);
        } catch (IOException e) {
            ServerMain.LOGGER.error("Problem with OutputStream...");
        }
    }

    private void closeConnection() {
        server.unsubscribe(this);
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            ServerMain.LOGGER.error("Error closing the connection...");
        }
    }

    @Override
    public void run() {
        try {
            if (authentication()) {
                readMessages();
            }
        } catch (IOException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            ServerMain.LOGGER.error("Client error...");
        } finally {
            closeConnection();
        }
    }
}
