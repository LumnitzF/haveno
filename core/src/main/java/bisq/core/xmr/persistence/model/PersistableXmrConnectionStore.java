package bisq.core.xmr.persistence.model;

import bisq.common.persistence.PersistenceManager;
import bisq.common.proto.persistable.PersistableEnvelope;
import bisq.common.proto.persistable.PersistedDataHost;

import protobuf.EncryptedMoneroConnection;
import protobuf.EncryptedMoneroConnectionStore;

import com.google.protobuf.Message;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class PersistableXmrConnectionStore implements PersistableEnvelope, PersistedDataHost {

    transient private PersistenceManager<PersistableXmrConnectionStore> persistenceManager;

    private final List<PersistableXmrDaemonConnection> items = new CopyOnWriteArrayList<>();

    @Inject
    public PersistableXmrConnectionStore(PersistenceManager<PersistableXmrConnectionStore> persistenceManager) {
        this.persistenceManager = persistenceManager;
        persistenceManager.initialize(this, "XmrDaemonConnections", PersistenceManager.Source.PRIVATE);
    }

    private PersistableXmrConnectionStore(List<PersistableXmrDaemonConnection> items) {
        this.items.addAll(items);
    }

    @Override
    public void readPersisted(Runnable completeHandler) {
        persistenceManager.readPersisted(persistedConnectionStore -> {
            items.clear();
            items.addAll(persistedConnectionStore.items);
            completeHandler.run();
        }, completeHandler);
    }

    public List<PersistableXmrDaemonConnection> getItems() {
        return ImmutableList.copyOf(items);
    }

    public void addConnection(PersistableXmrDaemonConnection connection) {
        // TODO
    }

    @Override
    public Message toProtoMessage() {
        List<EncryptedMoneroConnection> connections = items.stream().map(PersistableXmrDaemonConnection::toProtoMessage).collect(Collectors.toList());
        return protobuf.PersistableEnvelope.newBuilder()
                .setXmrConnectionStore(EncryptedMoneroConnectionStore.newBuilder()
                        .addAllItems(connections))
                .build();
    }

    public static PersistableXmrConnectionStore fromProto(protobuf.EncryptedMoneroConnectionStore proto) {
        List<PersistableXmrDaemonConnection> items = proto.getItemsList().stream()
                .map(PersistableXmrDaemonConnection::fromProto)
                .collect(Collectors.toList());
        return new PersistableXmrConnectionStore(items);
    }
}
