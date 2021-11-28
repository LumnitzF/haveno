package bisq.core.xmr.connection;

import bisq.core.xmr.connection.model.MoneroConnection;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Duration;

import java.net.URI;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;



import monero.common.MoneroError;
import monero.common.MoneroRpcConnection;

@Slf4j
@Singleton
public class MoneroConnectionManager {

    // TODO: add synchronisation

    private final monero.common.MoneroConnectionManager connectionManager;

    @Inject
    public MoneroConnectionManager(monero.common.MoneroConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void addConnection(MoneroConnection connection) {
        try {
            connectionManager.addConnection(toMoneroRpcConnection(connection));
        } catch (MoneroError error) {
            // TODO: connection already exists
        }
    }

    public void removeConnection(MoneroConnection connection) {
        removeConnection(connection.getUri());
    }

    public void removeConnection(URI uri) {
        try {
            connectionManager.removeConnection(uri.toString());
        } catch (MoneroError error) {
            // TODO: connection did not exist
        }
    }

    public MoneroConnection getConnection() {
        return toMoneroConnection(connectionManager.getConnection());
    }

    public List<MoneroConnection> getConnections() {
        return connectionManager.getConnections().stream().map(this::toMoneroConnection).collect(Collectors.toList());
    }

    public void setConnection(URI connectionUri) {
        connectionManager.setConnection(connectionUri.toString());
    }

    public void setConnection(MoneroConnection connection) {
        connectionManager.setConnection(toMoneroRpcConnection(connection));
    }

    public MoneroConnection checkConnection() {
        connectionManager.checkConnection();
        return getConnection();
    }

    public MoneroConnection checkConnection(MoneroConnection connection) {
        MoneroRpcConnection rpcConnection = toMoneroRpcConnection(connection);
        rpcConnection.checkConnection(connectionManager.getTimeout());
        return toMoneroConnection(rpcConnection);
    }

    public List<MoneroConnection> checkConnections() {
        connectionManager.checkConnections();
        return getConnections();
    }

    public void startCheckingConnection(Duration refreshPeriod) {
        connectionManager.startCheckingConnection(refreshPeriod == null ? null : refreshPeriod.toMillis());
    }

    public void stopCheckingConnection() {
        connectionManager.stopCheckingConnection();
    }

    public MoneroConnection getBestAvailableConnection() {
        return toMoneroConnection(connectionManager.getBestAvailableConnection());
    }

    public void setAutoSwitch(boolean autoSwitch) {
        connectionManager.setAutoSwitch(autoSwitch);
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
