package nl.vv32.musicapp.network.core;

import generated.Network;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Optional;

public class Connection {

    final private static Logger LOGGER = LoggerFactory.getLogger(Connection.class);

    final private SocketChannel channel;
    final private int connectionId;

    final private SingleSubject<DisconnectReason> onDisconnect;
    final private MessageReader reader;
    final private MessageWriter writer;
    final private PacketParser parser;
    final private PacketBuilder builder;

    public Connection(SocketChannel channel, int connectionId) {
        this.channel = channel;
        this.connectionId = connectionId;

        onDisconnect = SingleSubject.create();
        reader = new MessageReader();
        writer = new MessageWriter();
        parser = new PacketParser();
        builder = new PacketBuilder();
    }

    public Single<DisconnectReason> onDisconnect() {
        return onDisconnect;
    }
    public Flowable<Network.TestMessage> getTestMessages() {
        return parser.getTestMessages();
    }

    public Flowable<Network.LoginRequest> getLoginRequests() {
        return parser.getLoginRequests();
    }

    public Flowable<Network.LoginResponse> getLoginResponses() {
        return parser.getLoginResponses();
    }

    public void sendTestMessage(Network.TestMessage testMessage) {
        send(builder.build(testMessage));
    }

    public void sendLoginRequest(Network.LoginRequest loginRequest) {
        send(builder.build(loginRequest));
    }

    public void sendLoginResponse(Network.LoginResponse loginResponse) {
        send(builder.build(loginResponse));
    }

    public void read() {
        try {
            reader.read(channel::read, parser::parse);
        }
        catch (EOFException e) {
            disconnect(DisconnectReason.END_OF_STREAM);
        }
        catch (StreamCorruptedException e) {
            disconnect(DisconnectReason.STREAM_CORRUPTED);
        }
        catch (IOException e) {
            disconnect(DisconnectReason.IO_EXCEPTION);
        }
    }

    public void send(Network.Packet packet) {
        try {
            writer.write(channel::write, packet);
        }
        catch(IOException e) {
            disconnect(DisconnectReason.IO_EXCEPTION);
        }
    }

    private void disconnect(DisconnectReason reason) {
        //TODO channel not closed properly?
        try {
            channel.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        onDisconnect.onSuccess(reason);
    }

    public int getId() {
        return connectionId;
    }

    public Optional<SocketAddress> getAddress() {
        try {
            return Optional.of(channel.getRemoteAddress());
        }
        catch (IOException e) {
            return Optional.empty();
        }
    }
}
