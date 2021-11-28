package bisq.core.api;

import bisq.core.btc.setup.WalletConfig;
import bisq.core.util.Initializable;
import bisq.core.xmr.connection.MoneroConnectionManager;
import bisq.core.xmr.connection.model.MoneroConnection;
import bisq.core.xmr.connection.persistence.MoneroConnectionStore;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Duration;

import java.net.URI;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class CoreMoneroConnectionsService implements Initializable {

    private final MoneroConnectionManager connectionManager;
    private final MoneroConnectionStore connectionStore;

    @Inject
    public CoreMoneroConnectionsService(MoneroConnectionManager connectionManager,
                                        MoneroConnectionStore connectionStore) {
        this.connectionManager = connectionManager;
        this.connectionStore = connectionStore;
    }

    @Override
    public void initialize() {
        loadConnectionsFromStore();
        addDefaultConnection();
    }

    private void loadConnectionsFromStore() {
        connectionStore.getAllConnections().forEach(connectionManager::addConnection);
    }

    private void addDefaultConnection() {
        URI defaultUri = URI.create(WalletConfig.MONERO_DAEMON_URI);
        if (!connectionStore.hasConnection(defaultUri)) {
            addConnection(MoneroConnection.builder()
                    .uri(defaultUri)
                    .username(WalletConfig.MONERO_DAEMON_USERNAME)
                    .password(WalletConfig.MONERO_DAEMON_PASSWORD)
                    .build());
        }
    }

    void addConnection(MoneroConnection connection) {
        connectionStore.addConnection(connection);
        connectionManager.addConnection(connection);
    }

    void removeConnection(URI connectionUri) {
        connectionStore.removeConnection(connectionUri);
        connectionManager.removeConnection(connectionUri);
    }

    public void removeConnection(MoneroConnection connection) {
        removeConnection(connection.getUri());
    }

    MoneroConnection getConnection() {
        return connectionManager.getConnection();
    }

    public List<MoneroConnection> getConnections() {
        return connectionManager.getConnections();
    }

    public void setConnection(URI connectionUri) {
        connectionManager.setConnection(connectionUri);
    }

    public void setConnection(MoneroConnection connection) {
        connectionManager.setConnection(connection);
    }

    public MoneroConnection checkConnection() {
        return connectionManager.checkConnection();
    }

    public MoneroConnection checkConnection(MoneroConnection connection) {
        return connectionManager.checkConnection(connection);
    }

    public List<MoneroConnection> checkConnections() {
        return connectionManager.checkConnections();
    }

    public void startCheckingConnection(Duration refreshPeriod) {
        connectionManager.startCheckingConnection(refreshPeriod);
    }

    public void stopCheckingConnection() {
        connectionManager.stopCheckingConnection();
    }

    public MoneroConnection getBestAvailableConnection() {
        return connectionManager.getBestAvailableConnection();
    }

    public void setAutoSwitch(boolean autoSwitch) {
        connectionManager.setAutoSwitch(autoSwitch);
    }
}
