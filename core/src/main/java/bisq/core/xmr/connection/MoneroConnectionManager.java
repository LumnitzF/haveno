package bisq.core.xmr.connection;

import bisq.core.api.model.UriConnection;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;



import monero.common.MoneroRpcConnection;

@Slf4j
@Singleton
public class MoneroConnectionManager {

    private final Object lock = new Object();

    private final monero.common.MoneroConnectionManager connectionManager;

    @Inject
    public MoneroConnectionManager(monero.common.MoneroConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void addConnection(UriConnection connection) {
        synchronized (lock) {
            connectionManager.addConnection(toMoneroRpcConnection(connection));
        }
    }

    public void removeConnection(UriConnection connection) {
        synchronized (lock) {
            removeConnection(connection.getUri());
        }
    }

    public void removeConnection(String uri) {
        synchronized (lock) {
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
