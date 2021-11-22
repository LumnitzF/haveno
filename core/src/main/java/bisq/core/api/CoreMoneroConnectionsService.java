package bisq.core.api;

import bisq.core.xmr.daemon.connection.XmrDaemonConnectionManager;
import bisq.core.xmr.model.XmrDaemonConnection;
import bisq.core.xmr.persistence.XmrConnectionStore;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.net.URI;

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

    void addConnection(URI uri, String username, String password, int priority) {
        XmrDaemonConnection connection = XmrDaemonConnection.builder().uri(uri).username(username).password(password).priority(priority).build();
        connectionStore.addConnection(connection);
        daemonConnectionManager.addConnection(connection);
    }

}
