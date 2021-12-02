package bisq.core.xmr.connection.persistence.model;

import bisq.common.persistence.PersistenceManager;
import bisq.common.proto.persistable.PersistableEnvelope;
import bisq.common.proto.persistable.PersistedDataHost;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import javax.inject.Inject;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

public class XmrConnectionList implements PersistableEnvelope, PersistedDataHost {

    transient private final ReadWriteLock lock = new ReentrantReadWriteLock();

    transient private final Lock readLock = lock.readLock();
    transient private final Lock writeLock = lock.writeLock();

    transient private PersistenceManager<XmrConnectionList> persistenceManager;

    private byte[] salt;

    private final Map<String, EncryptedUriConnection> items = new HashMap<>();

    @Inject
    public XmrConnectionList(PersistenceManager<XmrConnectionList> persistenceManager) {
        this.persistenceManager = persistenceManager;
        persistenceManager.initialize(this, "XmrDaemonConnections", PersistenceManager.Source.PRIVATE);
    }

    private XmrConnectionList(byte[] salt, List<EncryptedUriConnection> items) {
        this.salt = salt;
        this.items.putAll(items.stream().collect(Collectors.toMap(EncryptedUriConnection::getUri, Function.identity())));
    }

    @Override
    public void readPersisted(Runnable completeHandler) {
        persistenceManager.readPersisted(persistedConnectionStore -> {
            writeLock.lock();
            try {
                salt = persistedConnectionStore.salt;
                items.clear();
                items.putAll(persistedConnectionStore.items);
            } finally {
                writeLock.unlock();
            }
            completeHandler.run();
        }, completeHandler);
    }

    public List<EncryptedUriConnection> getConnections() {
        readLock.lock();
        try {
            return ImmutableList.copyOf(items.values());
        } finally {
            readLock.unlock();
        }
    }

    public boolean hasConnection(String connection) {
        readLock.lock();
        try {
            return items.containsKey(connection);
        } finally {
            readLock.unlock();
        }
    }

    public void addConnection(EncryptedUriConnection connection) {
        EncryptedUriConnection currentValue;
        writeLock.lock();
        try {
            currentValue = items.putIfAbsent(connection.getUri(), connection);
        } finally {
            writeLock.unlock();
        }
        if (currentValue != null) {
            throw new IllegalStateException(String.format("There exists already an connection for \"%s\"", connection.getUri()));
        }
    }

    public void removeConnection(String connection) {
        writeLock.lock();
        try {
            items.remove(connection);
        } finally {
            writeLock.unlock();
        }
    }

    public byte[] getSalt() {
        readLock.lock();
        try {
            return salt;
        } finally {
            readLock.unlock();
        }
    }

    public void setSalt(byte[] salt) {
        writeLock.lock();
        try {
            this.salt = salt;
        } finally {
            writeLock.unlock();
        }
    }

    public void requestPersistence() {
        persistenceManager.requestPersistence();
    }

    public Lock getWriteLock() {
        return writeLock;
    }

    @Override
    public Message toProtoMessage() {
        List<protobuf.EncryptedUriConnection> connections;
        ByteString saltString;
        readLock.lock();
        try {
            connections = items.values().stream()
                    .map(EncryptedUriConnection::toProtoMessage).collect(Collectors.toList());
            saltString = ByteString.copyFrom(salt);
        } finally {
            readLock.unlock();
        }
        return protobuf.PersistableEnvelope.newBuilder()
                .setXmrConnectionList(protobuf.XmrConnectionList.newBuilder()
                        .setSalt(saltString)
                        .addAllItems(connections))
                .build();
    }

    public static XmrConnectionList fromProto(protobuf.XmrConnectionList proto) {
        List<EncryptedUriConnection> items = proto.getItemsList().stream()
                .map(EncryptedUriConnection::fromProto)
                .collect(Collectors.toList());
        return new XmrConnectionList(proto.getSalt().toByteArray(), items);
    }
}
