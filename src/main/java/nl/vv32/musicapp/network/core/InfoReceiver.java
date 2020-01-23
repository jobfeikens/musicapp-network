package nl.vv32.musicapp.network.core;

import generated.Network.RoomInfo;
import io.reactivex.rxjava3.processors.PublishProcessor;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.*;
import java.util.concurrent.*;

import static java.net.StandardSocketOptions.IP_MULTICAST_IF;

public class InfoReceiver extends NetworkRunnable {

    final private static int BUFFER_SIZE = 100;

    final private NetworkInterface networkInterface;
    final private DatagramChannel channel;
    final private Map<Integer, RoomInfo> infoMap;
    final private Map<Integer, Future<?>> taskMap;
    final private PublishProcessor<Pair<Integer, RoomInfo>> roomUpdates;
    final private PublishProcessor<Integer> roomRemovals;

    final private ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    final private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private MembershipKey membershipKey;

    public InfoReceiver(NetworkInterface networkInterface) throws IOException {
        this.networkInterface = networkInterface;

        channel = DatagramChannel.open();
        infoMap = new HashMap<>();
        taskMap = new HashMap<>();

        roomUpdates = PublishProcessor.create();
        roomRemovals = PublishProcessor.create();

        channel.setOption(IP_MULTICAST_IF, networkInterface);
    }

    public void start(SocketAddress bindAddress, InetAddress group) throws IOException {
        channel.bind(bindAddress);
        membershipKey = channel.join(group, networkInterface);
    }

    @Override
    protected void loop() throws IOException {
        var senderAddress = (InetSocketAddress) channel.receive(buffer);

        buffer.flip();
        handleRoomInfo(RoomInfo.parseFrom(buffer), senderAddress);
        buffer.clear();
    }

    @Override
    protected void stop() throws IOException {

    }

    private void handleRoomInfo(RoomInfo roomInfo, InetSocketAddress address) {
        int roomHash = Objects.hash(address.hashCode(), roomInfo.getPort());

        Future<?> removeTask;

        if((removeTask = taskMap.remove(roomHash)) != null) {

            removeTask.cancel(true);
        }
        removeTask = scheduler.schedule(new RemoveTask(roomHash), 5, TimeUnit.SECONDS);
        taskMap.put(roomHash, removeTask);

        RoomInfo oldInfo = infoMap.get(roomHash);

        if (oldInfo == null || !oldInfo.equals(roomInfo)) {

            infoMap.put(roomHash, roomInfo);
            roomUpdates.onNext(Pair.of(roomHash, roomInfo));
        }
    }

    private class RemoveTask implements Runnable {

        final private int roomHash;

        public RemoveTask(int roomHash) {
            this.roomHash = roomHash;
        }

        @Override
        public void run() {
            taskMap.remove(roomHash);
            roomRemovals.onNext(roomHash);
        }
    }

    @Override
    protected boolean isClosed() {
        return false;
    }

    public static void main(String[] args) throws Throwable {

        InfoReceiver receiver = new InfoReceiver(NetworkInterface.getByName("enp4s0"));

        receiver.roomUpdates.subscribe(x -> System.out.println("ROOM ADDED "));
        receiver.roomRemovals.subscribe(x -> System.out.println("ROOM REMOVED"));

        receiver.start(new InetSocketAddress(1997), InetAddress.getByName("224.106.111.98"));
        receiver.run();
    }
}
