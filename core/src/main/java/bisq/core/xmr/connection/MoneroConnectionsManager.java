package bisq.core.xmr.connection;

import bisq.core.api.model.UriConnection;
import bisq.core.btc.setup.WalletConfig;
import bisq.core.xmr.connection.persistence.model.XmrConnectionList;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;



import monero.common.MoneroConnectionManager;
import monero.common.MoneroRpcConnection;

@Slf4j
@Singleton
public final class MoneroConnectionsManager {

    private final Object lock = new Object();

    private final MoneroConnectionManager connectionManager;
    private final XmrConnectionList connectionList;

    @Inject
    public MoneroConnectionsManager(MoneroConnectionManager connectionManager,
                                    XmrConnectionList connectionList) {
        this.connectionManager = connectionManager;
        this.connectionList = connectionList;
        initialize();
    }

    private void initialize() {
        synchronized (lock) {
            loadConnections();
            addDefaultConnection();
        }
    }

    private void loadConnections() {
        connectionList.getConnections().forEach(uriConnection ->
                connectionManager.addConnection(toMoneroRpcConnection(uriConnection)));
    }

    private void addDefaultConnection() {
        String defaultUri = WalletConfig.MONERO_DAEMON_URI;
        if (!connectionList.hasConnection(defaultUri)) {
            addConnection(UriConnection.builder()
                    .uri(defaultUri)
                    .username(WalletConfig.MONERO_DAEMON_USERNAME)
                    .password(WalletConfig.MONERO_DAEMON_PASSWORD)
                    .build());
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
            return connectionManager.getConnections().stream().map(this::toUriConnection).collect(Collectors.toList());
        }
    }

    public void setConnection(String connectionUri) {
        synchronized (lock) {
            connectionManager.setConnection(connectionUri);
        }
    }

    public void setConnection(UriConnection connection) {
        synchronized (lock) {
            if (connectionManager.getConnectionByUri(connection.getUri()) != null
                    && connection.getUsername() == null
                    && connection.getPassword() == null) {
                // If the connection by this uri is already known, and nothing of special value is set,
                // only switch to the connection without overwriting username/password
                // with default values
                setConnection(connection.getUri());
            } else {
                connectionManager.setConnection(toMoneroRpcConnection(connection));
            }
        }
    }

    public UriConnection checkConnection() {
        synchronized (lock) {
            connectionManager.checkConnection();
            return getConnection();
        }
    }

    public UriConnection checkConnection(UriConnection connection) {
        // No synchronisation needed, as no interaction with connectionManager
        MoneroRpcConnection rpcConnection = toMoneroRpcConnection(connection);
        rpcConnection.checkConnection(connectionManager.getTimeout());
        return toUriConnection(rpcConnection);
    }

    public List<UriConnection> checkConnections() {
        synchronized (lock) {
            connectionManager.checkConnections();
            return getConnections();
        }
    }

    public void startCheckingConnection(Long refreshPeriod) {
        synchronized (lock) {
            connectionManager.startCheckingConnection(refreshPeriod);
        }
    }

    public void stopCheckingConnection() {
        synchronized (lock) {
            connectionManager.stopCheckingConnection();
        }
    }

    public UriConnection getBestAvailableConnection() {
        synchronized (lock) {
            return toUriConnection(connectionManager.getBestAvailableConnection());
        }
    }

    public void setAutoSwitch(boolean autoSwitch) {
        synchronized (lock) {
            connectionManager.setAutoSwitch(autoSwitch);
        }
    }

    private UriConnection toUriConnection(MoneroRpcConnection moneroRpcConnection) {
        if (moneroRpcConnection == null) {
            return null;
        }
        UriConnection.AuthenticationStatus authenticationStatus;
        if (moneroRpcConnection.isAuthenticated() == null) {
            authenticationStatus = UriConnection.AuthenticationStatus.NO_AUTHENTICATION;
        } else if (moneroRpcConnection.isAuthenticated()) {
            authenticationStatus = UriConnection.AuthenticationStatus.AUTHENTICATED;
        } else {
            authenticationStatus = UriConnection.AuthenticationStatus.NOT_AUTHENTICATED;
        }
        return UriConnection.builder()
                .uri(moneroRpcConnection.getUri())
                .priority(moneroRpcConnection.getPriority())
                .online(moneroRpcConnection.isOnline())
                .authenticationStatus(authenticationStatus)
                .build();
    }

    private MoneroRpcConnection toMoneroRpcConnection(UriConnection uriConnection) {
        return new MoneroRpcConnection(uriConnection.getUri(), uriConnection.getUsername(), uriConnection.getPassword()).setPriority(uriConnection.getPriority());
    }
}
