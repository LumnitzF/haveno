package bisq.core.xmr.daemon.connection;

import bisq.core.xmr.model.XmrDaemonConnection;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Duration;

import java.net.URI;

import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;



import monero.common.MoneroConnectionManager;
import monero.common.MoneroError;
import monero.common.MoneroRpcConnection;

@Slf4j
@Singleton
public class XmrDaemonConnectionManager {

    // TODO: add synchronisation

    private final MoneroConnectionManager connectionManager;

    @Inject
    public XmrDaemonConnectionManager(MoneroConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void addConnection(XmrDaemonConnection connection) {
        try {
            connectionManager.addConnection(toMoneroRpcConnection(connection));
        } catch (MoneroError error) {
            // TODO: connection already exists
        }
    }

    public void removeConnection(XmrDaemonConnection connection) {
        removeConnection(connection.getUri());
    }

    public void removeConnection(URI uri) {
        try {
            connectionManager.removeConnection(uri.toString());
        } catch (MoneroError error) {
            // TODO: connection did not exist
        }
    }

    public XmrDaemonConnection getConnection() {
        return toXmrDaemonConnection(connectionManager.getConnection());
    }

    public List<XmrDaemonConnection> getConnections() {
        return connectionManager.getConnections().stream().map(this::toXmrDaemonConnection).collect(Collectors.toList());
    }

    public void setConnection(URI connectionUri) {
        connectionManager.setConnection(connectionUri.toString());
    }

    public void setConnection(XmrDaemonConnection connection) {
        connectionManager.setConnection(toMoneroRpcConnection(connection));
    }

    public XmrDaemonConnection checkConnection() {
        connectionManager.checkConnection();
        return getConnection();
    }

    public XmrDaemonConnection checkConnection(XmrDaemonConnection connection) {
        MoneroRpcConnection rpcConnection = toMoneroRpcConnection(connection);
        rpcConnection.checkConnection(connectionManager.getTimeout());
        return toXmrDaemonConnection(rpcConnection);
    }

    public List<XmrDaemonConnection> checkConnections() {
        connectionManager.checkConnections();
        return getConnections();
    }

    public void startCheckingConnection(Duration refreshPeriod) {
        connectionManager.startCheckingConnection(refreshPeriod == null ? null : refreshPeriod.toMillis());
    }

    public void stopCheckingConnection() {
        connectionManager.stopCheckingConnection();
    }

    public XmrDaemonConnection getBestAvailableConnection() {
        return toXmrDaemonConnection(connectionManager.getBestAvailableConnection());
    }

    public void setAutoSwitch(boolean autoSwitch) {
        connectionManager.setAutoSwitch(autoSwitch);
    }

    private XmrDaemonConnection toXmrDaemonConnection(MoneroRpcConnection moneroRpcConnection) {
        XmrDaemonConnection.AuthenticationStatus authenticationStatus;
        if (moneroRpcConnection.getPassword() == null || moneroRpcConnection.isAuthenticated() == null) {
            authenticationStatus = XmrDaemonConnection.AuthenticationStatus.NO_AUTHENTICATION;
        } else if (moneroRpcConnection.isAuthenticated()) {
            authenticationStatus = XmrDaemonConnection.AuthenticationStatus.AUTHENTICATED;
        } else {
            authenticationStatus = XmrDaemonConnection.AuthenticationStatus.NOT_AUTHENTICATED;
        }
        return XmrDaemonConnection.builder()
                .uri(URI.create(moneroRpcConnection.getUri()))
                .priority(moneroRpcConnection.getPriority())
                .online(moneroRpcConnection.isOnline())
                .authenticationStatus(authenticationStatus)
                .build();
    }

    private MoneroRpcConnection toMoneroRpcConnection(XmrDaemonConnection xmrDaemonConnection) {
        return new MoneroRpcConnection(xmrDaemonConnection.getUri(), xmrDaemonConnection.getUsername(), xmrDaemonConnection.getPassword()).setPriority(xmrDaemonConnection.getPriority());
    }
}
