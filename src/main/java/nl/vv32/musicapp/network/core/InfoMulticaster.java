package nl.vv32.musicapp.network.core;

import generated.Network;
import io.reactivex.rxjava3.disposables.Disposable;
import nl.vv32.musicapp.network.server.Server;
import nl.vv32.musicapp.network.server.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import static java.net.StandardSocketOptions.IP_MULTICAST_IF;

public class InfoMulticaster extends NetworkRunnable {

    final private static Logger LOGGER = LoggerFactory.getLogger(InfoMulticaster.class);
    final private static int BUFFER_SIZE = 256;

    final private SocketAddress address;
    final private Server server;
    final private String roomName;
    final private UserManager userManager;
    final private ByteBuffer byteBuffer;

    private DatagramChannel channel;
    private Disposable loginSubscription;
    private Disposable logoutSubscription;

    public InfoMulticaster(SocketAddress address, Server server, String roomName) {
        super(1000);

        this.address = address;
        this.server = server;
        this.roomName = roomName;

        userManager = server.getUserManager();
        byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        updateInfo();
    }

    public void open() throws IOException {
        if (channel == null) {
            channel = DatagramChannel.open();
        } else {
            throw new IOException("Channel already open");
        }
        // Update the room info when a user logs in or out
        loginSubscription = userManager.getLogins()
                .subscribe(u -> updateInfo());

        logoutSubscription = userManager.getLogouts()
                .subscribe(u -> updateInfo());

        LOGGER.info(LogStrings.START_MULTICAST, address);
    }

    @Override
    public boolean isClosed() {
        return !channel.isOpen();
    }

    @Override
    public void loop() throws IOException {

        for (var i = NetworkInterface.getNetworkInterfaces().asIterator(); i.hasNext(); ) {

            var networkInterface = i.next();

            if (!networkInterface.isUp() || networkInterface.isLoopback()) {
                continue;
            }
            channel.setOption(IP_MULTICAST_IF, networkInterface);

            synchronized (byteBuffer) {
                byteBuffer.flip();
                channel.send(byteBuffer, address);
            }
        }
    }

    private Network.RoomInfo.Builder builder = Network.RoomInfo.newBuilder();

    private void updateInfo() {

        builder.setName(roomName)
                .setPort(server.getPort())
                .setCapacity(userManager.getCapacity())
                .setUserCount(userManager.getUserCount());

        synchronized (byteBuffer) {
            byteBuffer.clear();
            byteBuffer.put(builder.build().toByteArray());
        }
    }

    @Override
    public void stop() throws IOException {
        if(channel != null) {

            loginSubscription.dispose();
            logoutSubscription.dispose();

            loginSubscription = null;
            logoutSubscription = null;

            try {
                channel.close();
            } finally {
                channel = null;
            }
            LOGGER.info(LogStrings.STOP_MULTICAST);
        }
    }
}
