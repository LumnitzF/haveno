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

    private final Object lock = new Object();

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
        synchronized (lock) {
            loadConnectionsFromStore();
            addDefaultConnection();
        }
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
        synchronized (lock) {
            connectionStore.addConnection(connection);
            connectionManager.addConnection(connection);
        }
    }

    void removeConnection(URI connectionUri) {
        synchronized (lock) {
            connectionStore.removeConnection(connectionUri);
            connectionManager.removeConnection(connectionUri);
        }
    }

    public void removeConnection(MoneroConnection connection) {
        synchronized (lock) {
            removeConnection(connection.getUri());
        }
    }

    MoneroConnection getConnection() {
        synchronized (lock) {
            return connectionManager.getConnection();
        }
    }

    public List<MoneroConnection> getConnections() {
        synchronized (lock) {
            return connectionManager.getConnections();
        }
    }

    public void setConnection(URI connectionUri) {
        synchronized (lock) {
            connectionManager.setConnection(connectionUri);
        }
    }

    public void setConnection(MoneroConnection connection) {
        synchronized (lock) {
            connectionManager.setConnection(connection);
        }
    }

    public MoneroConnection checkConnection() {
        synchronized (lock) {
            return connectionManager.checkConnection();
        }
    }

    public MoneroConnection checkConnection(MoneroConnection connection) {
        synchronized (lock) {
            return connectionManager.checkConnection(connection);
        }
    }

    public List<MoneroConnection> checkConnections() {
        synchronized (lock) {
            return connectionManager.checkConnections();
        }
    }

    public void startCheckingConnection(Duration refreshPeriod) {
        synchronized (lock) {
            connectionManager.startCheckingConnection(refreshPeriod);
        }
    }

    public void stopCheckingConnection() {
        synchronized (lock) {
            connectionManager.stopCheckingConnection();
        }
    }

    public MoneroConnection getBestAvailableConnection() {
        synchronized (lock) {
            return connectionManager.getBestAvailableConnection();
        }
    }

    public void setAutoSwitch(boolean autoSwitch) {
        synchronized (lock) {
            connectionManager.setAutoSwitch(autoSwitch);
        }
    }
}
