package nl.vv32.musicapp.network.core;

import generated.Network;
import generated.Network.Packet;
import generated.Network.Packet.Builder;

import java.util.function.Function;

public class PacketBuilder {

    final private Builder builder = Packet.newBuilder();

    public Network.Packet build(Network.TestMessage testMessage) {
        return build(builder::setTestMessage, testMessage);
    }

    public Network.Packet build(Network.LoginRequest loginRequest) {
        return build(builder::setLoginRequest, loginRequest);
    }

    public Network.Packet build(Network.LoginResponse response) {
        return build(builder::setLoginResponse, response);
    }

    private <T> Network.Packet build(Function<T, Builder> function, T message) {
        builder.clear();
        function.apply(message);
        return builder.build();
    }
}
