package bisq.core.xmr.daemon.connection.persistence.model;

import bisq.common.persistence.PersistenceManager;
import bisq.common.proto.persistable.PersistableEnvelope;
import bisq.common.proto.persistable.PersistedDataHost;

import protobuf.EncryptedMoneroConnection;
import protobuf.EncryptedMoneroConnectionStore;

import com.google.protobuf.Message;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import java.net.URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PersistableXmrConnectionStore implements PersistableEnvelope, PersistedDataHost {

    transient private PersistenceManager<PersistableXmrConnectionStore> persistenceManager;

    private final Map<URI, PersistableXmrDaemonConnection> items = new HashMap<>();

    @Inject
    public PersistableXmrConnectionStore(PersistenceManager<PersistableXmrConnectionStore> persistenceManager) {
        this.persistenceManager = persistenceManager;
        persistenceManager.initialize(this, "XmrDaemonConnections", PersistenceManager.Source.PRIVATE);
    }

    private PersistableXmrConnectionStore(List<PersistableXmrDaemonConnection> items) {
        this.items.putAll(items.stream().collect(Collectors.toMap(PersistableXmrDaemonConnection::getUri, Function.identity())));
    }

    @Override
    public void readPersisted(Runnable completeHandler) {
        persistenceManager.readPersisted(persistedConnectionStore -> {
            items.clear();
            items.putAll(persistedConnectionStore.items);
            completeHandler.run();
        }, completeHandler);
    }

    public List<PersistableXmrDaemonConnection> getConnections() {
        return ImmutableList.copyOf(items.values());
    }

    public boolean hasConnection(URI connection) {
        return items.containsKey(connection);
    }

    public void addConnection(PersistableXmrDaemonConnection connection) {
        PersistableXmrDaemonConnection currentValue = items.putIfAbsent(connection.getUri(), connection);
        if (currentValue != null) {
            throw new IllegalStateException(String.format("There exists already an connection for \"%s\"", connection.getUri()));
        }
    }

    public void removeConnection(URI connection) {
        items.remove(connection);
    }

    public void requestPersistence() {
        persistenceManager.requestPersistence();
    }

    @Override
    public Message toProtoMessage() {
        // TODO: does this need to be synchronized?
        List<EncryptedMoneroConnection> connections = items.values().stream()
                .map(PersistableXmrDaemonConnection::toProtoMessage).collect(Collectors.toList());
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
