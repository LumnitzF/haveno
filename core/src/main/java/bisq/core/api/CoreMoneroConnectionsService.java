package bisq.core.api;

import bisq.core.btc.setup.WalletConfig;
import bisq.core.util.Initializable;
import bisq.core.xmr.daemon.connection.XmrDaemonConnectionManager;
import bisq.core.xmr.daemon.connection.model.XmrDaemonConnection;
import bisq.core.xmr.daemon.connection.persistence.XmrConnectionStore;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Duration;

import java.net.URI;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class CoreMoneroConnectionsService implements Initializable {

    private final XmrDaemonConnectionManager daemonConnectionManager;
    private final XmrConnectionStore connectionStore;

    @Inject
    public CoreMoneroConnectionsService(XmrDaemonConnectionManager daemonConnectionManager,
                                        XmrConnectionStore connectionStore) {
        this.daemonConnectionManager = daemonConnectionManager;
        this.connectionStore = connectionStore;
    }

    @Override
    public void initialize() {
        loadConnectionsFromStore();
        addDefaultConnection();
    }

    private void loadConnectionsFromStore() {
        connectionStore.getAllConnections().forEach(daemonConnectionManager::addConnection);
    }

    private void addDefaultConnection() {
        URI defaultUri = URI.create(WalletConfig.MONERO_DAEMON_URI);
        if (!connectionStore.hasConnection(defaultUri)) {
            addConnection(XmrDaemonConnection.builder()
                    .uri(defaultUri)
                    .username(WalletConfig.MONERO_DAEMON_USERNAME)
                    .password(WalletConfig.MONERO_DAEMON_PASSWORD)
                    .build());
        }
    }

    void addConnection(XmrDaemonConnection connection) {
        connectionStore.addConnection(connection);
        daemonConnectionManager.addConnection(connection);
    }

    void removeConnection(URI connectionUri) {
        connectionStore.removeConnection(connectionUri);
        daemonConnectionManager.removeConnection(connectionUri);
    }

    public void removeConnection(XmrDaemonConnection connection) {
        removeConnection(connection.getUri());
    }

    XmrDaemonConnection getConnection() {
        return daemonConnectionManager.getConnection();
    }

    public List<XmrDaemonConnection> getConnections() {
        return daemonConnectionManager.getConnections();
    }

    public void setConnection(URI connectionUri) {
        daemonConnectionManager.setConnection(connectionUri);
    }

    public void setConnection(XmrDaemonConnection connection) {
        daemonConnectionManager.setConnection(connection);
    }

    public XmrDaemonConnection checkConnection() {
        return daemonConnectionManager.checkConnection();
    }

    public XmrDaemonConnection checkConnection(XmrDaemonConnection connection) {
        return daemonConnectionManager.checkConnection(connection);
    }

    public List<XmrDaemonConnection> checkConnections() {
        return daemonConnectionManager.checkConnections();
    }

    public void startCheckingConnection(Duration refreshPeriod) {
        daemonConnectionManager.startCheckingConnection(refreshPeriod);
    }

    public void stopCheckingConnection() {
        daemonConnectionManager.stopCheckingConnection();
    }

    public XmrDaemonConnection getBestAvailableConnection() {
        return daemonConnectionManager.getBestAvailableConnection();
    }

    public void setAutoSwitch(boolean autoSwitch) {
        daemonConnectionManager.setAutoSwitch(autoSwitch);
    }
}
