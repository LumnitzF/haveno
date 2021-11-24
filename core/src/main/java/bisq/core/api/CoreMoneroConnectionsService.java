package bisq.core.api;

import bisq.core.xmr.daemon.connection.XmrDaemonConnectionManager;
import bisq.core.xmr.model.XmrDaemonConnection;
import bisq.core.xmr.persistence.XmrConnectionStore;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.time.Duration;

import java.net.URI;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
class CoreMoneroConnectionsService {

    private final XmrDaemonConnectionManager daemonConnectionManager;
    private final XmrConnectionStore connectionStore;

    @Inject
    public CoreMoneroConnectionsService(XmrDaemonConnectionManager daemonConnectionManager,
                                        XmrConnectionStore connectionStore) {
        this.daemonConnectionManager = daemonConnectionManager;
        this.connectionStore = connectionStore;
    }

    void addConnection(URI connectionUri, String username, String password, int priority) {
        XmrDaemonConnection connection = XmrDaemonConnection.builder().uri(connectionUri).username(username).password(password).priority(priority).build();
        addConnection(connection);
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
