package nl.vv32.musicapp.network.core;

import generated.Network;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.processors.PublishProcessor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;

public class Client extends NetworkRunnable {

    final private SocketChannel channel;
    final private Selector selector;
    final private PublishProcessor<Connection> connections;

    private Connection connection;

    public Client() throws IOException {
        channel = SocketChannel.open();
        selector = Selector.open();
        connections = PublishProcessor.create();

        channel.configureBlocking(false);
        channel.register(selector, OP_CONNECT | OP_READ);
    }

    public void connect(InetSocketAddress remote) throws IOException {
        channel.connect(remote);
    }

    public Single<Connection> onConnect() {
        return connections.firstOrError();
    }

    public Single<DisconnectReason> onDisconnect() {
        return connections.flatMapSingle(Connection::onDisconnect).firstOrError();
    }

    public Flowable<Network.TestMessage> getTestMessages() {
        return connections.flatMap(Connection::getTestMessages);
    }

    public Flowable<Network.LoginRequest> getLoginRequests() {
        return connections.flatMap(Connection::getLoginRequests);
    }

    public Flowable<Network.LoginResponse> getLoginResponses() {
        return connections.flatMap(Connection::getLoginResponses);
    }

    @Override
    public void loop() throws IOException {
        selector.select();

        for (var key : selector.selectedKeys()) {

            if (key.isReadable()) {
                connection.read();
            }
            else if (key.isConnectable()) {
                finishConnect();
            }
        }
        selector.selectedKeys().clear();
    }

    @Override
    public void stop() throws IOException {
        channel.close();
    }

    @Override
    public boolean isClosed() {
        return !channel.isOpen();
    }

    private void finishConnect() throws IOException {
        channel.finishConnect();
        connection = new Connection(channel, 0);
        connections.onNext(connection);
    }
}
