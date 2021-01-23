public class Main {
    public static void main(String[] args) {
        Server server;
        if (args.length <= 0) {
            server = new Server();
        } else {
            server = new Server(Integer.parseInt(args[0]));
        }
        server.start();
    }
}