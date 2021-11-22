package bisq.core.xmr.daemon.connection;

import bisq.core.xmr.model.XmrDaemonConnection;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.net.URI;

import lombok.extern.slf4j.Slf4j;

import org.jetbrains.annotations.NotNull;



import monero.common.MoneroConnectionManager;
import monero.common.MoneroError;
import monero.common.MoneroRpcConnection;

@Slf4j
@Singleton
public class XmrDaemonConnectionManager {

    private final MoneroConnectionManager connectionManager;

    @Inject
    public XmrDaemonConnectionManager(MoneroConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void addConnection(XmrDaemonConnection connection) {
        try {
            connectionManager.addConnection(toInternalConnection(connection));
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

    @NotNull
    private MoneroRpcConnection toInternalConnection(XmrDaemonConnection connection) {
        return new MoneroRpcConnection(connection.getUri(), connection.getUsername(), connection.getPassword()).setPriority(connection.getPriority());
    }
}
