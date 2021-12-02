package bisq.core.api;

import bisq.core.api.model.UriConnection;
import bisq.core.btc.setup.WalletConfig;
import bisq.core.util.Initializable;
import bisq.core.xmr.connection.MoneroConnectionManager;
import bisq.core.xmr.connection.persistence.MoneroConnectionStore;

import javax.inject.Inject;
import javax.inject.Singleton;

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
        String defaultUri = WalletConfig.MONERO_DAEMON_URI;
        if (!connectionStore.hasConnection(defaultUri)) {
            addConnection(UriConnection.builder()
                    .uri(defaultUri)
                    .username(WalletConfig.MONERO_DAEMON_USERNAME)
                    .password(WalletConfig.MONERO_DAEMON_PASSWORD)
                    .build());
        }
    }

    void addConnection(UriConnection connection) {
        synchronized (lock) {
            connectionStore.addConnection(connection);
            connectionManager.addConnection(connection);
        }
    }

    void removeConnection(String connectionUri) {
        synchronized (lock) {
            connectionStore.removeConnection(connectionUri);
            connectionManager.removeConnection(connectionUri);
        }
    }

    public void removeConnection(UriConnection connection) {
        synchronized (lock) {
            removeConnection(connection.getUri());
        }
    }

    UriConnection getConnection() {
        synchronized (lock) {
            return connectionManager.getConnection();
        }
    }

    List<UriConnection> getConnections() {
        synchronized (lock) {
            return connectionManager.getConnections();
        }
    }

    void setConnection(String connectionUri) {
        synchronized (lock) {
            connectionManager.setConnection(connectionUri);
        }
    }

    void setConnection(UriConnection connection) {
        synchronized (lock) {
            connectionManager.setConnection(connection);
        }
    }

    UriConnection checkConnection() {
        synchronized (lock) {
            return connectionManager.checkConnection();
        }
    }

    UriConnection checkConnection(UriConnection connection) {
        synchronized (lock) {
            return connectionManager.checkConnection(connection);
        }
    }

    List<UriConnection> checkConnections() {
        synchronized (lock) {
            return connectionManager.checkConnections();
        }
    }

    void startCheckingConnection(Long refreshPeriod) {
        synchronized (lock) {
            connectionManager.startCheckingConnection(refreshPeriod);
        }
    }

    void stopCheckingConnection() {
        synchronized (lock) {
            connectionManager.stopCheckingConnection();
        }
    }

    UriConnection getBestAvailableConnection() {
        synchronized (lock) {
            return connectionManager.getBestAvailableConnection();
        }
    }

    void setAutoSwitch(boolean autoSwitch) {
        synchronized (lock) {
            connectionManager.setAutoSwitch(autoSwitch);
        }
    }
}
