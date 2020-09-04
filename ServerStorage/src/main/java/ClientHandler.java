import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ClientHandler implements Runnable {

    private static final String PATH = "Server/OutFiles/";
    private final ServerMain server;
    private final Socket socket;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final List <String> comands = Arrays.asList("./upload", "./download", "./delete", "./contents");

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
        while(true) {
            String msgFromClient = inputStream.readUTF();
            for(String comand : comands) {
                if(msgFromClient.startsWith(comand)) {
                    switch(comand) {
                        case "./upload":
                            receivedFile();
                            ServerMain.LOGGER.error("File received! OK");
                            break;
                        case "./download":
                        case "./delete":
                            break;
                        case "./contents":
                            readFiles(new File(PATH));
                            break;
                    }
                }
            }
        }
    }

    private void receivedFile() {
        try {
            ServerMain.LOGGER.error("Started getting file...");
            String fileName = inputStream.readUTF();
            long fileLength = inputStream.readLong();
            File file = new File(PATH + fileName);
            if(file.createNewFile()) {
                try(FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[256];
                    if(fileLength < 256) {
                        fileLength += 256;
                    }
                    int read = 0;
                    for(int i = 0; i < fileLength / 256; i++) {
                        read = inputStream.read(buffer);
                        fos.write(buffer, 0, read);
                    }
                }
            }
        } catch(IOException e) {
            ServerMain.LOGGER.error("Error getting file...");
        }
    }

    public void readFiles(File baseDirectory) throws IOException {
        if(baseDirectory.isDirectory()) {
            for(File file : Objects.requireNonNull(baseDirectory.listFiles())) {
                if(file.isFile()) {
                    assert false;
                    outputStream.writeUTF(file.getName());
                }
            }
            outputStream.writeUTF("OK");
        }
    }

    @Override
    public void run() {
        try {
            readMessages();
        } catch(IOException e) {
            ServerMain.LOGGER.error("Client error...");
        }

    }
}
