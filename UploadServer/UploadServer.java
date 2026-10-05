import java.net.*;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UploadServer {

    private static final Semaphore connectionSemaphore = new Semaphore(3);

    private static final ExecutorService threadPool =
        Executors.newFixedThreadPool(3);

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(8082);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 8082.");
            System.exit(-1);
        }

        while (true) {

            try {
                connectionSemaphore.acquire();

                Socket socket = serverSocket.accept();

                threadPool.execute(
                    new UploadServerThread(socket, connectionSemaphore)
                );

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}