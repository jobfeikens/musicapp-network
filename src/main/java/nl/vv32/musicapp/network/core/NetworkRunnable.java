package nl.vv32.musicapp.network.core;

import java.io.IOException;

public abstract class NetworkRunnable<A> {

    final private long loopDelay;

    public NetworkRunnable(long loopDelay) {
        this.loopDelay = loopDelay;
    }

    public NetworkRunnable() {
        this(0);
    }

    public Void run() throws IOException, InterruptedException {
        try {
            while(!isClosed()) {
                loop();
                Thread.sleep(1000);
            }
        } finally {
            stop();
        }
        return null;
    }

    protected abstract void loop() throws IOException;

    protected abstract void stop() throws IOException;

    protected abstract boolean isClosed();
}
