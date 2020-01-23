package nl.vv32.musicapp.network.core;

import generated.Network;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.processors.PublishProcessor;

public class PacketParser {

    final private PublishProcessor<Network.TestMessage> testEvents = PublishProcessor.create();
    final private PublishProcessor<Network.LoginRequest> loginRequestEvents = PublishProcessor.create();
    final private PublishProcessor<Network.LoginResponse> loginResponseEvents = PublishProcessor.create();

    public void parse(Network.Packet packet) {
        switch(packet.getMessageCase()) {
            case TESTMESSAGE:
                testEvents.onNext(packet.getTestMessage());
                break;
            case LOGINREQUEST:
                loginRequestEvents.onNext(packet.getLoginRequest());
                break;
            case LOGINRESPONSE:
                loginResponseEvents.onNext(packet.getLoginResponse());
                break;
        }
    }

    public Flowable<Network.TestMessage> getTestMessages() {
        return testEvents;
    }

    public Flowable<Network.LoginRequest> getLoginRequests() {
        return loginRequestEvents;
    }

    public Flowable<Network.LoginResponse> getLoginResponses() {
        return loginResponseEvents;
    }
}
