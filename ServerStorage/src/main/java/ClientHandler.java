import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientHandler implements Runnable {

    private String PATH = "C:/";
    private static final String PATH_DB = "jdbc:sqlite:ServerStorage/src/main/resources/CloudStorage.db";
    private String name;
    private Long user_id;
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
        readFiles(new File(Objects.requireNonNull(getPath())));
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
            if(!Files.exists(Paths.get(PATH))) {
                File dir = new File(PATH);
                if(dir.mkdir())
                    ServerMain.LOGGER.info("Directory created successfully!");
                else
                    ServerMain.LOGGER.error("Error creating folder!");
            }
            File file = new File(PATH + fileName);
            if(file.exists()) file.delete();
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
            connection = DriverManager.getConnection(PATH_DB);
            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM" +
                    " Users WHERE login = ?");
            ps.setString(1, parts[1]);
            ResultSet rs = ps.executeQuery();
            connection.commit();
            if (passwordHandler.validatePassword(parts[2], rs.getInt(3), rs.getString(4), rs.getString(5))) {
                sendMessage("./auth ok " + parts[1]);
                user_id = rs.getLong(1);
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
        String hashString = passwordHandler.createHash(parts[2]);
        String[] param = hashString.split(":");
        try {
            connection = DriverManager.getConnection(PATH_DB);
            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement("INSERT INTO Users (login, iteration, salt, hash) VALUES (?,?,?,?)");
            ps.setString(1, parts[1]);
            ps.setString(2, param[0]);
            ps.setString(3, param[1]);
            ps.setString(4, param[2]);
            ps.executeUpdate();
            connection.commit();
            connection.close();
            name = parts[1];
            setPath();
        } catch (SQLException throwable) {
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

    private String getPath () {
        try {
            connection = DriverManager.getConnection(PATH_DB);
            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement("SELECT filePath FROM Files WHERE user_ID = ?");
            ps.setLong(1, user_id);
            ResultSet rs = ps.executeQuery();
            connection.commit();
            PATH = rs.getString(1);
            connection.close();
            return PATH;
        } catch (SQLException e) {
            e.getStackTrace();
        }
        return null;
    }

    private void setPath () {
        try {
            PATH = PATH + name + "/";
            connection = DriverManager.getConnection(PATH_DB);
            connection.setAutoCommit(false);
            PreparedStatement ps = connection.prepareStatement("SELECT User_ID FROM Users WHERE login = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            user_id = rs.getLong(1);
            ps = connection.prepareStatement("INSERT INTO Files (User_ID, filePath) VALUES (?,?)");
            ps.setLong(1, rs.getLong(1));
            ps.setString(2, PATH);
            ps.executeUpdate();
            connection.commit();
            connection.close();
        } catch (SQLException e) {
            e.getStackTrace();
        }
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
