package nl.vv32.musicapp.network.core;

import com.google.protobuf.InvalidProtocolBufferException;
import generated.Network.Packet;
import nl.vv32.jobutil.io.ByteQueue;

import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

final class MessageReader {

    final private ByteBuffer readBuffer = ByteBuffer.allocateDirect(256);
    final private ByteQueue queue = new ByteQueue();

    void read(ReadFunction readFunction, Consumer<Packet> messageCallback) throws IOException {

        // Throw an exception when the end of stream is reached
        if(readFunction.read(readBuffer) == -1) {
            throw new EOFException();
        }

        readBuffer.flip();
        queue.add(readBuffer.limit(), readBuffer::get);
        readBuffer.clear();

        while(true) {
            var bytes = queue.peekInt().flatMap(length -> queue.peekBytes(Integer.BYTES, length));

            if(bytes.isPresent()) {
                // Remove the parsed bytes from the queue
                queue.remove(Integer.BYTES + bytes.get().length);

                try {
                    //create new packet
                    Packet packet = Packet.parseFrom(bytes.get());

                    // Call back the message
                    messageCallback.accept(packet);
                }
                catch (InvalidProtocolBufferException e) {
                    throw new StreamCorruptedException();
                }
            } else {
                break;
            }
        }
    }

    @FunctionalInterface
    public interface ReadFunction {

        int read(ByteBuffer dst) throws IOException;
    }
}
