package nl.vv32.musicapp.network.core;

import generated.Network;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.Arrays;

public class Receive {

    public static void main(String[] args) throws IOException, InterruptedException {

        NetworkInterface ni = NetworkInterface.getByName("enp4s0");

        InetAddress group = InetAddress.getByName("224.106.111.98");

        DatagramChannel channel = DatagramChannel
                .open()
                .bind(new InetSocketAddress(1997))
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);

        MembershipKey key = channel.join(group, ni);

        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        while (true) {
            if (key.isValid()) {
                InetSocketAddress sa = (InetSocketAddress) channel.receive(byteBuffer);
                byteBuffer.flip();

                Network.RoomInfo info = Network.RoomInfo.parseFrom(byteBuffer);

                System.out.println(info);

                byteBuffer.clear();

                // TODO: Parse message
            }
        }
    }
}
