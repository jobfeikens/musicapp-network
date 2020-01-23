package nl.vv32.musicapp.network.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import generated.Network;
import generated.Network.*;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.PublishProcessor;
import nl.vv32.musicapp.network.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Server extends NetworkRunnable {

    final private static Logger LOGGER = LoggerFactory.getLogger(Server.class);

    final private ServerSocketChannel serverChannel;
    final private Selector selector;
    final private PublishProcessor<Connection> connections = PublishProcessor.create();
    final private BiMap<SelectionKey, Connection> connectionMap = HashBiMap.create();

    final private UserManager userManager;

    private int connectionCounter = 0;

    public Server(int capacity) throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.register(selector = Selector.open(), SelectionKey.OP_ACCEPT);

        userManager = new UserManager(this, capacity);
    }

    public int bind(SocketAddress address) throws IOException {
        userManager.start();

        serverChannel.bind(address);
        int actualPort = serverChannel.socket().getLocalPort();
        LOGGER.info(LogStrings.OPENED_SERVER, serverChannel.getLocalAddress());
        return actualPort;
    }

    @Override
    protected void loop() throws IOException {
        selector.select();

        for (SelectionKey key : selector.selectedKeys()) {

            if(!key.isValid()) {
                continue;
            }
            if (key.isReadable()) {
                read(key);
            }
            else if (key.isAcceptable()) {
                accept();
            }
        }
        selector.selectedKeys().clear();
    }

    @Override
    protected boolean isClosed() {
        return !serverChannel.isOpen();
    }

    @Override
    protected void stop() throws IOException {
        serverChannel.close(); //todo test if closed already
        LOGGER.info(LogStrings.CLOSED_SERVER);
    }

    private void accept() {
        try {
            SocketChannel channel = serverChannel.accept();

            channel.configureBlocking(false);

            SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);

            Connection connection = new Connection(channel, ++connectionCounter);

            connection.onDisconnect().subscribe(reason -> onDisconnect(connection, reason));

            connectionMap.put(readKey, connection);

            connections.onNext(connection);

            LOGGER.debug(LogStrings.CONNECTED_SERVER, connection.getId());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Flowable<Connection> onConnect() {
        return connections;
    }

    public Flowable<Pair<DisconnectReason, Connection>> onDisconnect() {
        return flatmapSingle(Connection::onDisconnect);
    }

    public Flowable<Pair<TestMessage, Connection>> getTestMessages() {
        return flatmap(Connection::getTestMessages);
    }

    public Flowable<Pair<LoginRequest, Connection>> getLoginRequests() {
        return flatmap(Connection::getLoginRequests);
    }

    private <T> Flowable<Pair<T, Connection>> flatmap(Function<Connection, Flowable<T>> mapper) {
        return connections.flatMap(connection -> mapper.apply(connection).map(x -> Pair.of(x, connection)));
    }

    private <T> Flowable<Pair<T, Connection>> flatmapSingle(Function<Connection, Single<T>> mapper) {
        return connections.flatMapSingle(connection -> mapper.apply(connection).map(x -> Pair.of(x, connection)));
    }

    public void sendTestMessage(Network.TestMessage testMessage, Target target) {
        send(Connection::sendTestMessage, testMessage, target);
    }

    public void sendLoginResponse(Network.LoginResponse loginResponse, Target target) {
        send(Connection::sendLoginResponse, loginResponse, target);
    }

    private void read(SelectionKey readKey) {
        connectionMap.get(readKey).read();
    }

    private <T> void send(BiConsumer<Connection, T> sendAction, T message, Target target) {
        connectionMap.values()
                .stream()
                .filter(target::doesTarget)
                .forEach(connection -> sendAction.accept(connection, message));
    }

    private void onDisconnect(Connection connection, DisconnectReason reason) {
        connectionMap.inverse()
                .remove(connection)
                .cancel();

        LOGGER.debug(LogStrings.DISCONNECTED_SERVER, connection.getId(), reason.toString());
    }

    public int getPort() {
        return serverChannel.socket().getLocalPort();
    }

    public UserManager getUserManager() {
        return userManager;
    }
}
