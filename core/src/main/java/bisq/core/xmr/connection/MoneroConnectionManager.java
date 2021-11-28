package bisq.core.xmr.connection;

import bisq.core.xmr.connection.model.MoneroConnection;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Duration;

import java.net.URI;

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

    public void addConnection(MoneroConnection connection) {
        synchronized (lock) {
            connectionManager.addConnection(toMoneroRpcConnection(connection));
        }
    }

    public void removeConnection(MoneroConnection connection) {
        synchronized (lock) {
            removeConnection(connection.getUri());
        }
    }

    public void removeConnection(URI uri) {
        synchronized (lock) {
            connectionManager.removeConnection(uri.toString());
        }
    }

    public MoneroConnection getConnection() {
        synchronized (lock) {
            return toMoneroConnection(connectionManager.getConnection());
        }
    }

    public List<MoneroConnection> getConnections() {
        synchronized (lock) {
            return connectionManager.getConnections().stream().map(this::toMoneroConnection).collect(Collectors.toList());
        }
    }

    public void setConnection(URI connectionUri) {
        synchronized (lock) {
            connectionManager.setConnection(connectionUri.toString());
        }
    }

    public void setConnection(MoneroConnection connection) {
        synchronized (lock) {
            connectionManager.setConnection(toMoneroRpcConnection(connection));
        }
    }

    public MoneroConnection checkConnection() {
        synchronized (lock) {
            connectionManager.checkConnection();
            return getConnection();
        }
    }

    public MoneroConnection checkConnection(MoneroConnection connection) {
        // No synchronisation needed, as no interaction with connectionManager
        MoneroRpcConnection rpcConnection = toMoneroRpcConnection(connection);
        rpcConnection.checkConnection(connectionManager.getTimeout());
        return toMoneroConnection(rpcConnection);
    }

    public List<MoneroConnection> checkConnections() {
        synchronized (lock) {
            connectionManager.checkConnections();
            return getConnections();
        }
    }

    public void startCheckingConnection(Duration refreshPeriod) {
        synchronized (lock) {
            connectionManager.startCheckingConnection(refreshPeriod == null ? null : refreshPeriod.toMillis());
        }
    }

    public void stopCheckingConnection() {
        synchronized (lock) {
            connectionManager.stopCheckingConnection();
        }
    }

    public MoneroConnection getBestAvailableConnection() {
        synchronized (lock) {
            return toMoneroConnection(connectionManager.getBestAvailableConnection());
        }
    }

    public void setAutoSwitch(boolean autoSwitch) {
        synchronized (lock) {
            connectionManager.setAutoSwitch(autoSwitch);
        }
    }

    private MoneroConnection toMoneroConnection(MoneroRpcConnection moneroRpcConnection) {
        MoneroConnection.AuthenticationStatus authenticationStatus;
        if (moneroRpcConnection.getPassword() == null || moneroRpcConnection.isAuthenticated() == null) {
            authenticationStatus = MoneroConnection.AuthenticationStatus.NO_AUTHENTICATION;
        } else if (moneroRpcConnection.isAuthenticated()) {
            authenticationStatus = MoneroConnection.AuthenticationStatus.AUTHENTICATED;
        } else {
            authenticationStatus = MoneroConnection.AuthenticationStatus.NOT_AUTHENTICATED;
        }
        return MoneroConnection.builder()
                .uri(URI.create(moneroRpcConnection.getUri()))
                .priority(moneroRpcConnection.getPriority())
                .online(moneroRpcConnection.isOnline())
                .authenticationStatus(authenticationStatus)
                .build();
    }

    private MoneroRpcConnection toMoneroRpcConnection(MoneroConnection moneroConnection) {
        return new MoneroRpcConnection(moneroConnection.getUri(), moneroConnection.getUsername(), moneroConnection.getPassword()).setPriority(moneroConnection.getPriority());
    }
}
