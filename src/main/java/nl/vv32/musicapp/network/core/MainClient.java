package nl.vv32.musicapp.network.core;

import generated.Network;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MainClient {

    public static void main(String... args) throws IOException, InterruptedException {

        Client client = new Client();

        client.connect(new InetSocketAddress("192.168.178.24", 25565));

        client.onConnect().subscribe(MainClient::onConnect);
        client.getLoginResponses().subscribe(MainClient::onLoginResonse);

        client.run();
    }

    private static void onConnect(Connection connection) {

        Network.LoginRequest request = Network.LoginRequest.newBuilder()
                .setUsername("jabakker")
                .build();

        connection.sendLoginRequest(request);
    }

    private static void onLoginResonse(Network.LoginResponse response) {
        System.out.println("GOT RESPONSE " + response.getStatus());
    }
}
