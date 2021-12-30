package bisq.core.api;

import bisq.core.api.model.UriConnection;
import bisq.core.xmr.connection.MoneroConnectionsManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class CoreMoneroConnectionsService {

    private final MoneroConnectionsManager connectionManager;

    @Inject
    public CoreMoneroConnectionsService(MoneroConnectionsManager connectionManager) {
        this.connectionManager = connectionManager;
    }


    void addConnection(UriConnection connection) {
        connectionManager.addConnection(connection);
    }

    void removeConnection(String connectionUri) {
        connectionManager.removeConnection(connectionUri);
    }

    void removeConnection(UriConnection connection) {
        removeConnection(connection.getUri());
    }

    UriConnection getConnection() {
        return connectionManager.getConnection();
    }

    List<UriConnection> getConnections() {
        return connectionManager.getConnections();
    }

    void setConnection(String connectionUri) {
        connectionManager.setConnection(connectionUri);
    }

    void setConnection(UriConnection connection) {
        connectionManager.setConnection(connection);
    }

    UriConnection checkConnection() {
        return connectionManager.checkConnection();
    }

    UriConnection checkConnection(UriConnection connection) {
        return connectionManager.checkConnection(connection);
    }

    List<UriConnection> checkConnections() {
        return connectionManager.checkConnections();
    }

    void startCheckingConnection(Long refreshPeriod) {
        connectionManager.startCheckingConnection(refreshPeriod);
    }

    void stopCheckingConnection() {
        connectionManager.stopCheckingConnection();
    }

    UriConnection getBestAvailableConnection() {
        return connectionManager.getBestAvailableConnection();
    }

    void setAutoSwitch(boolean autoSwitch) {
        connectionManager.setAutoSwitch(autoSwitch);
    }
}
