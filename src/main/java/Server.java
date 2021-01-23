import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int DEFAULT_PORT = 9998;
    private static final int N_THREADS = 64;
    private static final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    private static int port;

    public Server(int port) {
        this.port = port;
    }

    public Server() {
        this(DEFAULT_PORT);
    }

    private static void processConnection(Socket socket) throws IOException {

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

        String requestLine = in.readLine();
        String[] parts = requestLine.split(" ");

        if (parts.length != 3) {
            out.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Lenght: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n")
                    .getBytes());
            out.flush();
            return;
        }

        String path = parts[1];
        if (!VALID_PATHS.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            return;
        }

        Path filePath = Path.of(".", "public", path);
        String mimeType = Files.probeContentType(filePath);

        if (path.equals("/classic.html")) {
            byte[] content = Files.readString(filePath).replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
            return;
        }

        long length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();

    }


    public void start() {
        ServerSocket serverSocket;
        ExecutorService service = Executors.newFixedThreadPool(N_THREADS);
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int i = 0;
        while (i < N_THREADS) {
            service.execute(() -> {
                try {
                    processConnection(serverSocket.accept());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

}