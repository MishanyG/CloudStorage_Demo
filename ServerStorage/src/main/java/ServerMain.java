import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private final int PORT = 8189;
    private List <ClientHandler> clients;
    static final Logger LOGGER = LogManager.getLogger();
    static ExecutorService executorServiceClient = Executors.newCachedThreadPool();

    public ServerMain() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            clients = new ArrayList <>();

            while (true) {
                LOGGER.info("The Server was launched.\nWaiting for connection...");
                Socket socket = serverSocket.accept();
                LOGGER.info("User is connected.");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Server error...");
        } finally {
        }
    }

    public static void main(String[] args) {
        new ServerMain();
    }

    public synchronized void subscribe(ClientHandler client) {
        clients.add(client);
    }

    public synchronized void unsubscribe(ClientHandler client) {
        clients.remove(client);
    }
}
