package nl.vv32.musicapp.network.core;

import com.google.protobuf.Any;
import generated.Network;

import java.util.Arrays;

class Main {

    public static void main(String... args) {

        Network.TestMessage test = Network.TestMessage.newBuilder()
                .setString("job")
                .build();

        System.out.println(Arrays.toString(test.toByteArray()));

        Network.Packet packet = Network.Packet.newBuilder().setField(Network.Packet.getDescriptor().findFieldByName("message"), test).build();

        System.out.println(Arrays.toString(packet.toByteArray()));
    }
}
