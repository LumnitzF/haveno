package bisq.core.xmr.connection.persistence.model;

import bisq.core.api.CoreAccountService;
import bisq.core.api.model.UriConnection;
import bisq.core.crypto.ScryptUtil;

import bisq.common.crypto.CryptoException;
import bisq.common.crypto.Encryption;
import bisq.common.persistence.PersistenceManager;
import bisq.common.proto.persistable.PersistableEnvelope;
import bisq.common.proto.persistable.PersistedDataHost;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import org.bitcoinj.crypto.KeyCrypterScrypt;

import javax.inject.Inject;

import javax.crypto.SecretKey;

import java.security.SecureRandom;

import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

/**
 * Store for {@link EncryptedUriConnection}s.
 * <p>
 * Passwords are encrypted when stored onto disk, using the account password.
 * If a connection has no password, this is "hidden" by using some random value as fake password.
 *
 * @implNote The password encryption mechanism is handled as follows.
 *  A random salt is generated and stored for each connection. If the connection has no password,
 *  the salt is used as prefix and some random data is attached as fake password. If the connection has a password,
 *  the salt is used as suffix to the actual password. When the password gets decrypted, it is checked whether the
 *  salt is a prefix of the decrypted value. If it is a prefix, the connection has no password.
 *  Otherwise, it is removed (from the end) and the remaining value is the actual password.
 */
public class XmrConnectionList implements PersistableEnvelope, PersistedDataHost {

    private static final int MIN_FAKE_PASSWORD_LENGTH = 5;
    private static final int MAX_FAKE_PASSWORD_LENGTH = 32;
    private static final int SALT_LENGTH = 16;

    transient private final ReadWriteLock lock = new ReentrantReadWriteLock();
    transient private final Lock readLock = lock.readLock();
    transient private final Lock writeLock = lock.writeLock();
    transient private final SecureRandom random = new SecureRandom();

    transient private KeyCrypterScrypt keyCrypterScrypt;
    transient private SecretKey encryptionKey;

    transient private CoreAccountService accountService;
    transient private PersistenceManager<XmrConnectionList> persistenceManager;

    private final Map<String, EncryptedUriConnection> items = new HashMap<>();

    @Inject
    public XmrConnectionList(PersistenceManager<XmrConnectionList> persistenceManager,
                             CoreAccountService accountService) {
        this.accountService = accountService;
        this.persistenceManager = persistenceManager;
        this.persistenceManager.initialize(this, "XmrConnectionList", PersistenceManager.Source.PRIVATE);
        this.accountService.addPasswordChangeListener(this::onPasswordChange);
    }

    private XmrConnectionList(byte[] salt, List<EncryptedUriConnection> items) {
        this.keyCrypterScrypt = ScryptUtil.getKeyCrypterScrypt(salt);
        this.items.putAll(items.stream().collect(Collectors.toMap(EncryptedUriConnection::getUri, Function.identity())));
    }

    @Override
    public void readPersisted(Runnable completeHandler) {
        persistenceManager.readPersisted(persistedXmrConnectionList -> {
            writeLock.lock();
            try {
                initializeEncryption(persistedXmrConnectionList.keyCrypterScrypt);
                items.clear();
                items.putAll(persistedXmrConnectionList.items);
            } finally {
                writeLock.unlock();
            }
            completeHandler.run();
        }, () -> {
            writeLock.lock();
            try {
                initializeEncryption(ScryptUtil.getKeyCrypterScrypt());
            } finally {
                writeLock.unlock();
            }
            completeHandler.run();
        });
    }

    private void initializeEncryption(KeyCrypterScrypt keyCrypterScrypt) {
        this.keyCrypterScrypt = keyCrypterScrypt;
        encryptionKey = toSecretKey(accountService.getPassword());
    }

    public List<UriConnection> getConnections() {
        readLock.lock();
        try {
            return items.values().stream().map(this::toUriConnection).collect(Collectors.toList());
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

    public void addConnection(UriConnection connection) {
        EncryptedUriConnection currentValue;
        writeLock.lock();
        try {
            EncryptedUriConnection encryptedUriConnection = toEncryptedUriConnection(connection);
            currentValue = items.putIfAbsent(connection.getUri(), encryptedUriConnection);
        } finally {
            writeLock.unlock();
        }
        if (currentValue != null) {
            throw new IllegalStateException(String.format("There exists already an connection for \"%s\"", connection.getUri()));
        }
        requestPersistence();
    }

    public void removeConnection(String connection) {
        writeLock.lock();
        try {
            items.remove(connection);
        } finally {
            writeLock.unlock();
        }
    }

    private void requestPersistence() {
        persistenceManager.requestPersistence();
    }

    private void onPasswordChange(String oldPassword, String newPassword) {
        writeLock.lock();
        try {
            SecretKey oldSecret = encryptionKey;
            assert Objects.equals(oldSecret, toSecretKey(oldPassword)) : "Old secret does not match old password";
            encryptionKey = toSecretKey(newPassword);
            items.replaceAll((key, connection) -> reEncrypt(connection, oldSecret, encryptionKey));
        } finally {
            writeLock.unlock();
        }
        requestPersistence();
    }

    private SecretKey toSecretKey(String password) {
        if (password == null) {
            return null;
        }
        return Encryption.getSecretKeyFromBytes(keyCrypterScrypt.deriveKey(password).getKey());
    }

    private static EncryptedUriConnection reEncrypt(EncryptedUriConnection connection,
                                                    SecretKey oldSecret, SecretKey newSecret) {
        return connection.toBuilder()
                .encryptedPassword(reEncrypt(connection.getEncryptedPassword(), oldSecret, newSecret))
                .build();
    }

    private static byte[] reEncrypt(byte[] value,
                                    SecretKey oldSecret, SecretKey newSecret) {
        // was previously not encrypted if null
        byte[] decrypted = oldSecret == null ? value : decrypt(value, oldSecret);
        // should not be encrypted if null
        return newSecret == null ? decrypted : encrypt(decrypted, newSecret);
    }

    private static byte[] decrypt(byte[] encrypted, SecretKey secret) {
        try {
            return Encryption.decrypt(encrypted, secret);
        } catch (CryptoException e) {
            throw new IllegalArgumentException("Illegal old password", e);
        }
    }

    private static byte[] encrypt(byte[] unencrypted, SecretKey secretKey) {
        try {
            return Encryption.encrypt(unencrypted, secretKey);
        } catch (CryptoException e) {
            throw new RuntimeException("Could not encrypt data with the provided secret", e);
        }
    }

    private EncryptedUriConnection toEncryptedUriConnection(UriConnection connection) {
        String password = connection.getPassword();
        byte[] passwordBytes = password == null ? null : password.getBytes(StandardCharsets.UTF_8);
        byte[] passwordSalt = generateSalt(passwordBytes);
        byte[] encryptedPassword = encryptPassword(passwordBytes, passwordSalt);

        return EncryptedUriConnection.builder()
                .uri(connection.getUri())
                .username(connection.getUsername())
                .priority(connection.getPriority())
                .encryptedPassword(encryptedPassword)
                .encryptionSalt(passwordSalt)
                .build();
    }

    private UriConnection toUriConnection(EncryptedUriConnection connection) {
        byte[] decryptedPasswordBytes = decryptPassword(connection.getEncryptedPassword(), connection.getEncryptionSalt());
        String password = decryptedPasswordBytes == null ? null : new String(decryptedPasswordBytes, StandardCharsets.UTF_8);
        return UriConnection.builder()
                .uri(connection.getUri())
                .username(connection.getUsername())
                .priority(connection.getPriority())
                .password(password)
                .build();
    }


    private byte[] encryptPassword(byte[] password, byte[] salt) {
        byte[] saltedPassword;
        if (password == null) {
            // no password given, so use salt as prefix and add some random data, which disguises itself as password
            int fakePasswordLength = random.nextInt(MAX_FAKE_PASSWORD_LENGTH - MIN_FAKE_PASSWORD_LENGTH + 1)
                    + MIN_FAKE_PASSWORD_LENGTH;
            byte[] fakePassword = new byte[fakePasswordLength];
            random.nextBytes(fakePassword);
            saltedPassword = new byte[salt.length + fakePasswordLength];
            System.arraycopy(salt, 0, saltedPassword, 0, salt.length);
            System.arraycopy(fakePassword, 0, saltedPassword, salt.length, fakePassword.length);
        } else {
            // password given, so append salt to end
            saltedPassword = new byte[password.length + salt.length];
            System.arraycopy(password, 0, saltedPassword, 0, password.length);
            System.arraycopy(salt, 0, saltedPassword, password.length, salt.length);
        }
        return encrypt(saltedPassword, encryptionKey);
    }

    @Nullable
    private byte[] decryptPassword(byte[] encryptedSaltedPassword, byte[] salt) {
        byte[] decryptedSaltedPassword = decrypt(encryptedSaltedPassword, encryptionKey);
        if (arrayStartsWith(decryptedSaltedPassword, salt)) {
            // salt is prefix, so no actual password set
            return null;
        } else {
            // remove salt suffix, the rest is the actual password
            byte[] decryptedPassword = new byte[decryptedSaltedPassword.length - salt.length];
            System.arraycopy(decryptedSaltedPassword, 0, decryptedPassword, 0, decryptedPassword.length);
            return decryptedPassword;
        }
    }

    private byte[] generateSalt(byte[] password) {
        byte[] salt = new byte[SALT_LENGTH];
        // Generate salt, that is guaranteed to be no prefix of the password
        do {
            random.nextBytes(salt);
        } while (arrayStartsWith(password, salt));
        return salt;
    }

    private static boolean arrayStartsWith(byte[] container, byte[] prefix) {
        if (container.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (container[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Message toProtoMessage() {
        List<protobuf.EncryptedUriConnection> connections;
        ByteString saltString;
        readLock.lock();
        try {
            connections = items.values().stream()
                    .map(EncryptedUriConnection::toProtoMessage).collect(Collectors.toList());
            saltString = keyCrypterScrypt.getScryptParameters().getSalt();
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
