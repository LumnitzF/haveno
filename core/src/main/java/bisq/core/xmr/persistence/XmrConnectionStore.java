package bisq.core.xmr.persistence;

import bisq.core.xmr.model.XmrDaemonConnection;
import bisq.core.xmr.persistence.model.PersistableXmrConnectionStore;
import bisq.core.xmr.persistence.model.PersistableXmrDaemonConnection;

public class XmrConnectionStore {

    // TODO: handle encryption / decryption of passwords

    PersistableXmrConnectionStore store = null;

    public void addConnection(XmrDaemonConnection connection) {
        PersistableXmrDaemonConnection persistableConnection = toPersistableConnection(connection);
        store.addConnection(persistableConnection);
    }

    private PersistableXmrDaemonConnection toPersistableConnection(XmrDaemonConnection connection) {
        return null;
    }
}
