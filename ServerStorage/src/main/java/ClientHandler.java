import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientHandler implements Runnable {

    private String PATH = "";
    private final ServerMain server;
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final List <String> commands = Arrays.asList("./upload", "./download", "./delete");

    public ClientHandler(ServerMain server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.inputStream = new DataInputStream(socket.getInputStream());
            this.outputStream = new DataOutputStream(socket.getOutputStream());
            ServerMain.executorServiceClient.execute(new Thread(this));
        } catch(IOException e) {
            throw new RuntimeException("Problems when you create handler of client.");
        }
    }

    public synchronized void readMessages() throws IOException {
        PATH = ClientServiceSQL.getPath();
        readFiles(new File(Objects.requireNonNull(PATH)));
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
        }
        outputStream.writeUTF("OK");
        outputStream.flush();
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

    public boolean authentication() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        String str;
        while ((str = inputStream.readUTF()) != null) {
            if (str.startsWith("./singUp")) {
                if (ClientServiceSQL.signUp(str)) {
                    sendMessage("./singUp passed");
                    break;
                } else {
                    sendMessage("./singUp failed");
                }
            }
            if (str.startsWith("./logIn")) {
                str = ClientServiceSQL.logIn(str);
                if (str.startsWith("./auth ok")) {
                    server.subscribe(this);
                    sendMessage("./logIn passed");
                    break;
                } else {
                    sendMessage(str);
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
