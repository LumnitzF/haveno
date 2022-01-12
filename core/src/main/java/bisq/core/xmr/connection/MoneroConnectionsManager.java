package bisq.core.xmr.connection;

import bisq.core.api.model.UriConnection;
import bisq.core.xmr.connection.persistence.model.EncryptedConnectionList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import monero.common.MoneroConnectionManager;
import monero.common.MoneroRpcConnection;

@Slf4j
@Singleton
public final class MoneroConnectionsManager {

    // TODO: this connection manager should update app status, don't poll in WalletsSetup every 30 seconds
    private static final long DEFAULT_REFRESH_PERIOD = 15_000L; // check the connection every 15 seconds per default

    // TODO (woodser): support each network type, move to config, remove localhost authentication
    private static final List<MoneroRpcConnection> DEFAULT_CONNECTIONS = Arrays.asList(
            new MoneroRpcConnection("http://localhost:38081", "superuser", "abctesting123").setPriority(1), // localhost is first priority
            new MoneroRpcConnection("http://haveno.exchange:38081", "", "").setPriority(2)
    );

    private final Object lock = new Object();
    private final MoneroConnectionManager connectionManager;
    private final EncryptedConnectionList connectionList;

    @Inject
    public MoneroConnectionsManager(MoneroConnectionManager connectionManager,
                                    EncryptedConnectionList connectionList) {
        this.connectionManager = connectionManager;
        this.connectionList = connectionList;
        initialize();
    }

    private void initialize() {
        synchronized (lock) {

            // load connections
            connectionList.getConnections().forEach(uriConnection -> {
                connectionManager.addConnection(toMoneroRpcConnection(uriConnection));
            });

            // add default connections
            for (MoneroRpcConnection connection : DEFAULT_CONNECTIONS) {
                if (connectionList.hasConnection(connection.getUri())) continue;
                addConnection(UriConnection.builder()
                        .uri(connection.getUri())
                        .username(connection.getUsername())
                        .password(connection.getPassword())
                        .priority(connection.getPriority())
                        .build());
            }

            // restore last used connection
            connectionList.getCurrentConnectionUri().ifPresentOrElse(connectionManager::setConnection, () -> {
                connectionManager.setConnection(DEFAULT_CONNECTIONS.get(0).getUri()); // default to localhost
            });

            // register connection change listener
            connectionManager.addListener(this::onConnectionChanged);

            // restore configuration
            connectionManager.setAutoSwitch(connectionList.getAutoSwitch());
            long refreshPeriod = connectionList.getRefreshPeriod();
            if (refreshPeriod > 0) connectionManager.startCheckingConnection(refreshPeriod);
            else if (refreshPeriod == 0) connectionManager.startCheckingConnection(DEFAULT_REFRESH_PERIOD);

            // check connection
            checkConnection();
        }
    }

    private void onConnectionChanged(MoneroRpcConnection currentConnection) {
        synchronized (lock) {
            if (currentConnection == null) {
                connectionList.setCurrentConnectionUri(null);
            } else {
                connectionList.removeConnection(currentConnection.getUri());
                connectionList.addConnection(toUriConnection(currentConnection, true));
                connectionList.setCurrentConnectionUri(currentConnection.getUri());
            }
        }
    }

    public void addConnection(UriConnection connection) {
        synchronized (lock) {
            connectionList.addConnection(connection);
            connectionManager.addConnection(toMoneroRpcConnection(connection));
        }
    }

    public void removeConnection(String uri) {
        synchronized (lock) {
            connectionList.removeConnection(uri);
            connectionManager.removeConnection(uri);
        }
    }

    public UriConnection getConnection() {
        synchronized (lock) {
            return toUriConnection(connectionManager.getConnection());
        }
    }

    public List<UriConnection> getConnections() {
        synchronized (lock) {
            return connectionManager.getConnections().stream().map(MoneroConnectionsManager::toUriConnection).collect(Collectors.toList());
        }
    }

    public void setConnection(String connectionUri) {
        synchronized (lock) {
            connectionManager.setConnection(connectionUri); // listener will update connection list
        }
    }

    public void setConnection(UriConnection connection) {
        synchronized (lock) {
            connectionManager.setConnection(toMoneroRpcConnection(connection)); // listener will update connection list
        }
    }

    public UriConnection checkConnection() {
        synchronized (lock) {
            connectionManager.checkConnection();
            return getConnection();
        }
    }

    public List<UriConnection> checkConnections() {
        synchronized (lock) {
            connectionManager.checkConnections();
            return getConnections();
        }
    }

    public void startCheckingConnection(Long refreshPeriod) {
        synchronized (lock) {
            connectionManager.startCheckingConnection(refreshPeriod == null ? DEFAULT_REFRESH_PERIOD : refreshPeriod);
            connectionList.setRefreshPeriod(refreshPeriod);
        }
    }

    public void stopCheckingConnection() {
        synchronized (lock) {
            connectionManager.stopCheckingConnection();
            connectionList.setRefreshPeriod(-1L);
        }
    }

    public UriConnection getBestAvailableConnection() {
        synchronized (lock) {
            return toUriConnection(connectionManager.getBestAvailableConnection(), false);
        }
    }

    public void setAutoSwitch(boolean autoSwitch) {
        synchronized (lock) {
            connectionManager.setAutoSwitch(autoSwitch);
            connectionList.setAutoSwitch(autoSwitch);
        }
    }

    private static UriConnection toUriConnection(MoneroRpcConnection rpcConnection) {
        return toUriConnection(rpcConnection, false);
    }

    private static UriConnection toUriConnection(MoneroRpcConnection rpcConnection, boolean includeCredentials) {
        if (rpcConnection == null) return null;
        return UriConnection.builder()
                .uri(rpcConnection.getUri())
                .priority(rpcConnection.getPriority())
                .onlineStatus(toOnlineStatus(rpcConnection.isOnline()))
                .authenticationStatus(toAuthenticationStatus(rpcConnection.isAuthenticated()))
                .username(includeCredentials ? rpcConnection.getUsername() : "")
                .password(includeCredentials ? rpcConnection.getPassword() : "")
                .build();
    }

    private static UriConnection.AuthenticationStatus toAuthenticationStatus(Boolean authenticated) {
        if (authenticated == null) return UriConnection.AuthenticationStatus.NO_AUTHENTICATION;
        else if (authenticated) return UriConnection.AuthenticationStatus.AUTHENTICATED;
        else return UriConnection.AuthenticationStatus.NOT_AUTHENTICATED;
    }

    private static UriConnection.OnlineStatus toOnlineStatus(Boolean online) {
        if (online == null) return UriConnection.OnlineStatus.UNKNOWN;
        else if (online) return UriConnection.OnlineStatus.ONLINE;
        else return UriConnection.OnlineStatus.OFFLINE;
    }

    private static MoneroRpcConnection toMoneroRpcConnection(UriConnection uriConnection) {
        if (uriConnection == null) return null;
        return new MoneroRpcConnection(uriConnection.getUri(), uriConnection.getUsername(), uriConnection.getPassword()).setPriority(uriConnection.getPriority());
    }
}
