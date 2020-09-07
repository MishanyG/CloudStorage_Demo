import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientHandler implements Runnable {

    private static final String PATH = "ServerStorage/OutFiles/";
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
                    int count = 0;
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

    @Override
    public void run() {
        try {
            readMessages();
        } catch(IOException e) {
            e.printStackTrace();
            ServerMain.LOGGER.error("Client error...");
        }

    }
}
