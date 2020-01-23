package nl.vv32.musicapp.network;

import nl.vv32.musicapp.network.core.InfoMulticaster;
import nl.vv32.musicapp.network.server.Server;
import nl.vv32.musicapp.network.server.UserManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Scanner;
import java.util.concurrent.*;

public class ServerApp {

    final private static SocketAddress MULTICAST_ADDRESS =
            new InetSocketAddress("224.106.111.98", 1997);

    final private SocketAddress address;
    final private ThreadPoolExecutor executor;
    final private Server server;
    final private InfoMulticaster multicaster;

    public ServerApp(SocketAddress address, int capacity) throws IOException {
        this.address = address;

        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2, new ThreadFactory());
        server = new Server(capacity);
        multicaster = new InfoMulticaster(MULTICAST_ADDRESS, server, "test room");
    }

    public void run() throws IOException {
        server.bind(address);
        multicaster.open();

        Future<Void> f1 = executor.submit(server::run);
        Future<Void> f2 = executor.submit(multicaster::run);

        try {
            new Scanner(System.in).nextLine();
        } finally {
            f1.cancel(true);
            f2.cancel(true);

            executor.shutdown();
        }
    }

    public static void main(String... args) throws IOException {
        ServerApp app = new ServerApp(new InetSocketAddress(25565), 10);
        app.run();
    }

    private static class ThreadFactory implements java.util.concurrent.ThreadFactory {

        private int threadCount = 0;

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "server" + "-" + ++threadCount);
        }
    }
}
