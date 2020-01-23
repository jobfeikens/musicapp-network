package nl.vv32.musicapp.network.core;

import generated.Network;

import java.net.InetSocketAddress;
import java.util.Objects;

public class Test {

    public static void main(String[] args) {
        Network.RoomInfo roomInfo = Network.RoomInfo.getDefaultInstance();

        System.out.println(roomInfo.equals(null));
    }
}
