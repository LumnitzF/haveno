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
            connectionManager.setConnection(toMoneroRpcConnection(connection));
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
        return UriConnection.builder()
                .uri(moneroRpcConnection.getUri())
                .priority(moneroRpcConnection.getPriority())
                .online(toOnlineStatus(moneroRpcConnection.isOnline()))
                .authenticationStatus(toAuthenticationStatus(moneroRpcConnection.isAuthenticated()))
                .build();
    }

    private UriConnection.AuthenticationStatus toAuthenticationStatus(Boolean authenticated) {
        if (authenticated == null) {
            return UriConnection.AuthenticationStatus.NO_AUTHENTICATION;
        } else if (authenticated) {
            return UriConnection.AuthenticationStatus.AUTHENTICATED;
        } else {
            return UriConnection.AuthenticationStatus.NOT_AUTHENTICATED;
        }
    }

    private UriConnection.OnlineStatus toOnlineStatus(Boolean online) {
        if (online == null) {
            return UriConnection.OnlineStatus.UNKNOWN;
        } else if (online) {
            return UriConnection.OnlineStatus.ONLINE;
        } else {
            return UriConnection.OnlineStatus.OFFLINE;
        }
    }

    private MoneroRpcConnection toMoneroRpcConnection(UriConnection uriConnection) {
        if (uriConnection == null) {
            return null;
        }
        return new MoneroRpcConnection(uriConnection.getUri(), uriConnection.getUsername(), uriConnection.getPassword()).setPriority(uriConnection.getPriority());
    }
}
