package nl.vv32.musicapp.network.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import generated.Network;
import generated.Network.LoginResponse;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.processors.PublishProcessor;
import nl.vv32.musicapp.network.core.Connection;
import nl.vv32.musicapp.network.core.DisconnectReason;
import nl.vv32.musicapp.network.core.LogStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static generated.Network.LoginResponse.Status.*;

public class UserManager implements Disposable {

    final private static Logger LOGGER = LoggerFactory.getLogger(UserManager.class);

    final private Server server;
    final private int capacity;

    final private BiMap<Connection, User> userMap;
    final private LoginResponse.Builder builder;
    final private PublishProcessor<User> logins;
    final private PublishProcessor<User> logouts;

    public UserManager(Server server, int capacity) {
        this.server = server;
        this.capacity = capacity;

        userMap = HashBiMap.create();
        builder = LoginResponse.newBuilder();
        logins = PublishProcessor.create();
        logouts = PublishProcessor.create();
    }

    private Disposable loginSubscription;
    private Disposable disconnectSubscription;

    public void start() {
        loginSubscription = server.getLoginRequests()
                .subscribe(pair -> pair.unpack(this::onLoginRequest));

        disconnectSubscription = server.onDisconnect()
                .subscribe(pair -> pair.unpack(this::onDisconnect));
    }

    public Flowable<User> getLogins() {
        return logins;
    }

    public Flowable<User> getLogouts() {
        return logins.flatMapSingle(User::onLogout);
    }

    private void onLoginRequest(Network.LoginRequest loginRequest, Connection connection) {
        String username = loginRequest.getUsername();

        if(userMap.size() >= capacity) {
            builder.setStatus(FULL);
        }
        else if(username.length() == 0) {
            builder.setStatus(NAME_TOO_SHORT);
        }
        else if(userExists(username)) {
            builder.setStatus(NAME_EXISTS);
        }
        else if(userMap.containsKey(connection)) {
            builder.setStatus(ALREADY_LOGGED_IN);
        }
        else {
            builder.setStatus(OK);
            User user = new User(connection, username);
            userMap.put(connection, user);
            LOGGER.info(LogStrings.LOGIN_SUCCESSFUL, username, user.getAddress().orElse(null));
            logins.onNext(user);
        }
        connection.sendLoginResponse(builder.build());
    }

    private boolean userExists(String username) {
        return false; //todo
    }

    private void onDisconnect(DisconnectReason reason, Connection connection) {
        User user = userMap.remove(connection);

        if(user != null) {
            LOGGER.info(LogStrings.LOGGED_OUT, user.getUsername(), reason.toString());
            user.logout();
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getUserCount() {
        return userMap.size();
    }

    public void stop() {
        Objects.requireNonNull(loginSubscription, "not started");
        Objects.requireNonNull(loginSubscription, "not started");

        loginSubscription.dispose();
        disconnectSubscription.dispose();

        loginSubscription = null;
        disconnectSubscription = null;
    }

    private void stop(Throwable throwable) {

    }

    private boolean disposed = false;

    @Override
    public void dispose() {
        if (!disposed) {
            logins.onComplete();
            disposed = true;
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
