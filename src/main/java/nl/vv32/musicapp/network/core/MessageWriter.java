package nl.vv32.musicapp.network.core;

import com.google.protobuf.Any;
import com.google.protobuf.MessageLite;
import generated.Network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MessageWriter {

    public void write(WriteFunction writeFunction, Network.Packet packet) throws IOException {
        int messageSize = packet.getSerializedSize();

        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES + messageSize);

        byteBuffer.putInt(messageSize);

        byteBuffer.put(packet.toByteArray());

        byteBuffer.flip();

        writeFunction.write(byteBuffer);
    }

    @FunctionalInterface
    public interface WriteFunction {

        void write(ByteBuffer src) throws IOException;
    }
}
