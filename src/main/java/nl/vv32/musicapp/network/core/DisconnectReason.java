package nl.vv32.musicapp.network.core;

public enum DisconnectReason {

    END_OF_STREAM("End of Stream"),
    STREAM_CORRUPTED("Stream corrupted"),
    CLOSED("Closed"),
    IO_EXCEPTION("Unknown I/O exception");

    final public String text;

    DisconnectReason(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
