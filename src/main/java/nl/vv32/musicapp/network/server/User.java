package nl.vv32.musicapp.network.server;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.SingleSubject;
import nl.vv32.musicapp.network.core.Connection;

import java.net.SocketAddress;
import java.util.Optional;

public class User {

    final private Connection connection;
    final private String username;

    final private SingleSubject<User> logoutSubject;

    public User(Connection connection, String username) {
        this.connection = connection;
        this.username = username;

        logoutSubject = SingleSubject.create();
    }

    public Connection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    public Optional<SocketAddress> getAddress() {
        return connection.getAddress();
    }

    public void logout() {
        logoutSubject.onSuccess(this);
    }

    public Single<User> onLogout() {
        return logoutSubject;
    }

}
