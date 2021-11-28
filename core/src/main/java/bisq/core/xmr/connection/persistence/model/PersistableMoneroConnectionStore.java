package bisq.core.xmr.connection.persistence.model;

import bisq.common.persistence.PersistenceManager;
import bisq.common.proto.persistable.PersistableEnvelope;
import bisq.common.proto.persistable.PersistedDataHost;

import protobuf.EncryptedMoneroConnection;
import protobuf.EncryptedMoneroConnectionStore;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import java.net.URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PersistableMoneroConnectionStore implements PersistableEnvelope, PersistedDataHost {

    transient private PersistenceManager<PersistableMoneroConnectionStore> persistenceManager;

    private byte[] salt;

    private final Map<URI, PersistableMoneroConnection> items = new HashMap<>();

    @Inject
    public PersistableMoneroConnectionStore(PersistenceManager<PersistableMoneroConnectionStore> persistenceManager) {
        this.persistenceManager = persistenceManager;
        persistenceManager.initialize(this, "XmrDaemonConnections", PersistenceManager.Source.PRIVATE);
    }

    private PersistableMoneroConnectionStore(byte[] salt, List<PersistableMoneroConnection> items) {
        this.salt = salt;
        this.items.putAll(items.stream().collect(Collectors.toMap(PersistableMoneroConnection::getUri, Function.identity())));
    }

    @Override
    public void readPersisted(Runnable completeHandler) {
        persistenceManager.readPersisted(persistedConnectionStore -> {
            salt = persistedConnectionStore.salt;
            items.clear();
            items.putAll(persistedConnectionStore.items);
            completeHandler.run();
        }, completeHandler);
    }

    public List<PersistableMoneroConnection> getConnections() {
        return ImmutableList.copyOf(items.values());
    }

    public boolean hasConnection(URI connection) {
        return items.containsKey(connection);
    }

    public void addConnection(PersistableMoneroConnection connection) {
        PersistableMoneroConnection currentValue = items.putIfAbsent(connection.getUri(), connection);
        if (currentValue != null) {
            throw new IllegalStateException(String.format("There exists already an connection for \"%s\"", connection.getUri()));
        }
    }

    public void removeConnection(URI connection) {
        items.remove(connection);
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public void requestPersistence() {
        persistenceManager.requestPersistence();
    }

    @Override
    public Message toProtoMessage() {
        // TODO: does this need to be synchronized?
        List<EncryptedMoneroConnection> connections = items.values().stream()
                .map(PersistableMoneroConnection::toProtoMessage).collect(Collectors.toList());
        return protobuf.PersistableEnvelope.newBuilder()
                .setXmrConnectionStore(EncryptedMoneroConnectionStore.newBuilder()
                        .setSalt(ByteString.copyFrom(salt))
                        .addAllItems(connections))
                .build();
    }

    public static PersistableMoneroConnectionStore fromProto(protobuf.EncryptedMoneroConnectionStore proto) {
        List<PersistableMoneroConnection> items = proto.getItemsList().stream()
                .map(PersistableMoneroConnection::fromProto)
                .collect(Collectors.toList());
        return new PersistableMoneroConnectionStore(proto.getSalt().toByteArray(), items);
    }
}
