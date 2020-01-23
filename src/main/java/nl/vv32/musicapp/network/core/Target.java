package nl.vv32.musicapp.network.core;

@FunctionalInterface
public interface Target {

    boolean doesTarget(Connection connection);

    static Target all() {
        return connection -> true;
    }

    static Target just(Connection connection) {
        return connection::equals;
    }

    static Target allExcept(Connection connection) {
        return other -> !other.equals(connection);
    }
}
